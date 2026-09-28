#!/usr/bin/env python3
import os, json, base64, subprocess, tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
enc=lambda s:base64.b64encode(s.encode()).decode()
cases=[('',c) for c in [
'cat fichier.txt | grep mot', 'grep mot fichier.txt | wc -l','printf "a\\nb\\n" | sort','cat fichier.txt | head -n 5','cat fichier.txt | tail -n 5',
'printf "3\\n1\\n2\\n" | sort -n | head -n 2','printf "%s\\n" b a b | sort | uniq -c','grep ABSENT fichier.txt && echo BAD || echo GOOD',
'false && echo BAD; echo GOOD','true || echo BAD; echo GOOD','false || true && echo GOOD','printf "a|b\\n" | grep -F "a|b"',
'echo "a && b"','echo "a > b"','echo "$HOME"',"echo '$HOME'",'printf "bonjour\\n"','printf "%s-%s\\n" a b c d',
'cat -n fichier.txt','wc -l fichier.txt','wc -c fichier.txt','grep -E "alpha|beta" fichier.txt','grep -F "alpha:10" fichier.txt',
"cut -d: -f1 fichier.txt", "sed -n '1,5p' fichier.txt", "awk '{print $1}' fichier.txt",'cat fichier.txt | tr a-z A-Z',
'cat fichier.txt | tee capture | wc -l','sha256sum fichier.txt','sha512sum fichier.txt','sha1sum fichier.txt','md5sum fichier.txt','cksum fichier.txt',
'sort -nd fichier.txt', 'cat absent.txt', 'rm -f absent.txt',
'cat absent.txt | wc -l', 'printf a; printf b',
]]
cases += [
('echo one > x; echo two >> x','cat x'),('printf "a\\nb\\n" > x','wc -l < x'),('cat fichier.txt | grep mot > result','cat result'),
('cp -v source.txt copie.txt','cat copie.txt'),('cp -a dossier copie','cat copie/file.txt'),('mv -v ancien.txt nouveau.txt','cat nouveau.txt'),
('mkdir -p spaced; touch "spaced/a b"; echo ok > "spaced/a b"','cat "spaced/a b"'),('rm -f fichier.txt','cat fichier.txt'),
('chmod 755 script.sh','stat script.sh'),('ln -s source.txt lien','cat lien'),('ln -s source.txt lien','readlink lien'),
('mkdir -p tmp/a/b; rmdir -p tmp/a/b','ls tmp'),('git add README.md; echo extra >> README.md','git diff --name-only'),
]
cases += [
('git restore README.md; git switch -c topic; echo topic > README.md; git add README.md; git commit -m topic; git switch main; git merge topic', 'cat README.md'),
('git restore README.md; git switch --detach HEAD; echo detached > README.md; git add README.md; git commit -m detached; git switch main', 'cat README.md'),
('git stash; echo new > untracked; git stash pop', 'cat README.md untracked'),
('git restore README.md; echo next > README.md; git add README.md; git commit -m next; git revert --no-edit HEAD', 'cat README.md'),
('git restore README.md; git switch -c topic; echo topic > README.md; git add README.md; git commit -m topic; git switch main; git cherry-pick topic', 'cat README.md'),
('git add README.md; echo next > README.md; git restore README.md', 'cat README.md'),
('git add README.md; git commit -m next; git reset --hard HEAD~1', 'cat README.md'),
('git add README.md; git commit -m next; git reset --soft HEAD~1', 'git diff --cached --name-only'),
('git add README.md; git commit -m next; git reset --mixed HEAD~1', 'git diff --name-only'),
]
# Exclude platform-dependent stat text from stdout equality, but preserve it in the report.
raw=''.join(enc(setup)+'\t'+enc(cmd)+'\n' for setup,cmd in cases)
env=os.environ.copy()
java=subprocess.run([os.environ.get('AUDIT_JAVA','java'),'-cp',os.environ.get('AUDIT_CLASSES',str(ROOT/'build/audit-classes')),'com.ghiles.quizubuntu.DifferentialProbe'],input=raw,text=True,capture_output=True,env=env,check=True)
assert len(java.stdout.splitlines()) == len(cases), 'Missing simulator results'
results=[]
sample='Git\ngit notes\nGitHub\nalpha:10\nbeta:2\nmot\nMOT\n\n\n2\n10\n2\n'
for (setup,cmd),line in zip(cases,java.stdout.splitlines()):
    status,kind,encoded=line.split('\t',2); actual=base64.b64decode(encoded).decode()
    with tempfile.TemporaryDirectory(dir=ROOT/'audit') as directory:
        p=Path(directory)
        for d in ['dossier/sub','src','empty','test','a/b/c']: (p/d).mkdir(parents=True,exist_ok=True)
        for f in ['README.md','fichier.txt','source.txt','ancien.txt','script.sh','a.txt','b.txt','fichier.log','source','dossier/file.txt']: (p/f).write_text(sample)
        ge={**os.environ,'LC_ALL':'C','HOME':'/home/ubuntu','GIT_CONFIG_NOSYSTEM':'1','GIT_CONFIG_GLOBAL':'/dev/null'}
        for args in [['git','init','-q','-b','main'],['git','config','user.name','Test'],['git','config','user.email','test@example.com'],['git','add','.'],['git','commit','-qm','base'],['git','branch','feature']]: subprocess.run(args,cwd=p,env=ge,check=True,capture_output=True)
        with (p/'README.md').open('a') as f:f.write('change\n')
        subprocess.run(['bash','-c',setup],cwd=p,env=ge,capture_output=True,timeout=5)
        oracle=subprocess.run(['bash','-c',cmd],cwd=p,env=ge,capture_output=True,timeout=5)
        expected=oracle.stdout.decode(errors='replace')
        equal=(int(status)==0 and actual==expected if oracle.returncode==0 else int(status)!=0)
        if cmd.startswith('stat '): equal='0755' in actual
        if cmd.startswith('echo ') and '$HOME' in cmd: equal=actual==expected
        results.append(dict(setup=setup,command=cmd,passed=equal,expected_exit=oracle.returncode,actual_exit=int(status),expected=expected,actual=actual,stderr=oracle.stderr.decode(errors='replace')))
(ROOT/'audit/scenarios.json').write_text(json.dumps(results,ensure_ascii=False,indent=2))
for r in results:
    if not r['passed']:print('FAIL',r['setup'],';',r['command'],'EXPECTED',repr(r['expected']),'ACTUAL',repr(r['actual']),r['actual_exit'])
print(f"{sum(r['passed'] for r in results)}/{len(results)} scenarios passed")

raise SystemExit(any(not r["passed"] for r in results))
