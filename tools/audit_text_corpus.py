#!/usr/bin/env python3
import os,sys,json,base64,subprocess,tempfile,shlex
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
rows=json.loads((ROOT/'audit/catalogue-results.json').read_text())
commands=[r['command'] for r in rows if r['category']=='Texte exécutable']
fixtures={
 'course':'Git\ngit notes\nGitHub\nalpha:10\nbeta:2\nmot\nMOT\n\n\n2\n10\n2\n',
 'empty':'',
 'no_final_newline':'Git\nGit\nlast',
 'numeric':'  2\n-10\n+3\n2.5\n.5\n02\nA\na\n a!\n',
 'unicode':'été\nÉté\nGit é\n🙂\n\té\n',
 'controls':'Git\tA\n\x01\x7f\n\n\nGit\n',
}
b64=lambda s:base64.b64encode(s.encode()).decode()
pairs=[(f,data,c) for f,data in fixtures.items() for c in commands]
payload=''.join(b64(c)+'\t'+b64(d)+'\n' for f,d,c in pairs)
env=os.environ.copy()
proc=subprocess.run([os.environ.get('AUDIT_JAVA','java'),'-cp',os.environ.get('AUDIT_CLASSES',str(ROOT/'build/audit-classes')),'com.ghiles.quizubuntu.TextCorpusProbe'],input=payload,text=True,capture_output=True,env=env,check=True)
assert len(proc.stdout.splitlines()) == len(pairs), 'Missing simulator results'
results=[];test_vectors=[]
with tempfile.TemporaryDirectory(dir=ROOT/'audit') as work:
    for (fixture,data,command),line in zip(pairs,proc.stdout.splitlines()):
        Path(work,'README.md').write_bytes(data.encode())
        reference=subprocess.run(shlex.split(command),cwd=work,env={**os.environ,'LC_ALL':'C.UTF-8'},capture_output=True,timeout=5)
        status,encoded=line.split('\t',1);actual=base64.b64decode(encoded).decode();expected=reference.stdout.decode(errors='replace')
        passed=int(status)==reference.returncode and actual==expected
        results.append(dict(fixture=fixture,command=command,passed=passed,expected_exit=reference.returncode,actual_exit=int(status),expected=expected,actual=actual))
        test_vectors.append('\t'.join([b64(command),b64(data),str(reference.returncode),b64(expected)]))
(ROOT/'audit/text-corpus.json').write_text(json.dumps(dict(total=len(results),passed=sum(r['passed'] for r in results),fixtures=list(fixtures),commands=len(commands),failures=[r for r in results if not r['passed']]),ensure_ascii=False,indent=2))
resource=ROOT/'app/src/test/resources/gnu-text-corpus.tsv';resource.parent.mkdir(parents=True,exist_ok=True);resource.write_text('\n'.join(test_vectors)+'\n')
failed=[r for r in results if not r['passed']]
print(f'{len(results)-len(failed)}/{len(results)} exact stdout and exit-code comparisons pass')
from collections import Counter
print('Failures by fixture/tool',Counter((r['fixture'],r['command'].split()[0]) for r in failed))
for r in failed[:10]:print(r['fixture'],r['command'],repr(r['expected']),repr(r['actual']),r['expected_exit'],r['actual_exit'])

sys.exit(bool(failed))
