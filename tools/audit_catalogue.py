#!/usr/bin/env python3
"""Execute every advertised example in a fresh, documented fixture; not a semantic proof."""
import os, json, base64, subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
proc=subprocess.run([os.environ.get('AUDIT_JAVA','java'),'-cp',os.environ.get('AUDIT_CLASSES',str(ROOT/'build/audit-classes')),'com.ghiles.quizubuntu.CatalogAuditProbe'],check=True,capture_output=True,text=True)
rows=[]
for line in proc.stdout.splitlines():
    command,category,kind,output,*status=line.split('\t')
    decode=lambda value:base64.b64decode(value).decode()
    message=decode(output)
    unsupported=any(x in message.lower() for x in ['non implément','non prise en charge','non pris en charge','pas pris en charge','non disponible','confirmation interactive requise','commande introuvable'])
    rows.append(dict(command=decode(command),category=decode(category),kind=kind,output=message,exit_code=int(status[0]) if status else None,assessment='limitation explicite' if unsupported else 'exécuté, conformité complète non certifiée'))
(ROOT/'audit').mkdir(exist_ok=True)
(ROOT/'audit/catalogue-results.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2))
print(f'{len(rows)} examples executed; {sum(r["kind"]=="CRASH" for r in rows)} crashes; {sum(r["assessment"]=="limitation explicite" for r in rows)} explicit limitations')
raise SystemExit(any(r['kind']=='CRASH' for r in rows))
