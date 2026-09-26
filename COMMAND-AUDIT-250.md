# Ubuntu Lab — audit automatisé de 250 commandes

Cette liste est la batterie de régression prioritaire exécutée par GitHub Actions avant chaque APK. Chaque commande est lancée dans une machine Ubuntu/Git virtuelle préparée ; le test échoue si elle plante, tombe sur « commande introuvable », « non prise en charge » ou sur le simple fallback documentaire. Des tests supplémentaires vérifient les workflows Bash, branches/checkout, remote/push, conflit/merge et SSH.

| # | Commande | Contrôle CI |
|---:|---|---|
| 1 | `pwd` | Exécution comportementale |
| 2 | `ls` | Exécution comportementale |
| 3 | `ls -l` | Exécution comportementale |
| 4 | `ls -la` | Exécution comportementale |
| 5 | `ls -al` | Exécution comportementale |
| 6 | `cd ~` | Exécution comportementale |
| 7 | `cd ..` | Exécution comportementale |
| 8 | `cd -` | Exécution comportementale |
| 9 | `mkdir test` | Exécution comportementale |
| 10 | `mkdir -p a/b/c` | Exécution comportementale |
| 11 | `touch file.txt` | Exécution comportementale |
| 12 | `echo hello` | Exécution comportementale |
| 13 | `echo hello > file.txt` | Exécution comportementale |
| 14 | `echo again >> file.txt` | Exécution comportementale |
| 15 | `cat README.md` | Exécution comportementale |
| 16 | `nano README.md` | Exécution comportementale |
| 17 | `cp README.md copy.md` | Exécution comportementale |
| 18 | `cp -r src src-copy` | Exécution comportementale |
| 19 | `mv README.md README2.md` | Exécution comportementale |
| 20 | `rm temp.txt` | Exécution comportementale |
| 21 | `rm -r src-copy` | Exécution comportementale |
| 22 | `rmdir empty` | Exécution comportementale |
| 23 | `clear` | Exécution comportementale |
| 24 | `history` | Exécution comportementale |
| 25 | `whoami` | Exécution comportementale |
| 26 | `hostname` | Exécution comportementale |
| 27 | `uname` | Exécution comportementale |
| 28 | `uname -a` | Exécution comportementale |
| 29 | `id` | Exécution comportementale |
| 30 | `date` | Exécution comportementale |
| 31 | `alias` | Exécution comportementale |
| 32 | `alias gs='git status'` | Exécution comportementale |
| 33 | `unalias ll` | Exécution comportementale |
| 34 | `env` | Exécution comportementale |
| 35 | `printenv` | Exécution comportementale |
| 36 | `printenv HOME` | Exécution comportementale |
| 37 | `export TEST=1` | Exécution comportementale |
| 38 | `echo $HOME` | Exécution comportementale |
| 39 | `printf "hello\n"` | Exécution comportementale |
| 40 | `head README.md` | Exécution comportementale |
| 41 | `head -n 1 README.md` | Exécution comportementale |
| 42 | `tail README.md` | Exécution comportementale |
| 43 | `tail -n 1 README.md` | Exécution comportementale |
| 44 | `wc README.md` | Exécution comportementale |
| 45 | `wc -l README.md` | Exécution comportementale |
| 46 | `wc -w README.md` | Exécution comportementale |
| 47 | `wc -c README.md` | Exécution comportementale |
| 48 | `grep Base README.md` | Exécution comportementale |
| 49 | `grep -i base README.md` | Exécution comportementale |
| 50 | `grep -n Base README.md` | Exécution comportementale |
| 51 | `grep -v nope README.md` | Exécution comportementale |
| 52 | `find .` | Exécution comportementale |
| 53 | `find . -type f` | Exécution comportementale |
| 54 | `find . -type d` | Exécution comportementale |
| 55 | `find . -name "*.md"` | Exécution comportementale |
| 56 | `which git` | Exécution comportementale |
| 57 | `command -v git` | Exécution comportementale |
| 58 | `whereis git` | Exécution comportementale |
| 59 | `type git` | Exécution comportementale |
| 60 | `basename /home/ubuntu/work/README.md` | Exécution comportementale |
| 61 | `dirname /home/ubuntu/work/README.md` | Exécution comportementale |
| 62 | `realpath .` | Exécution comportementale |
| 63 | `file README.md` | Exécution comportementale |
| 64 | `stat README.md` | Exécution comportementale |
| 65 | `du` | Exécution comportementale |
| 66 | `du -h` | Exécution comportementale |
| 67 | `du -sh .` | Exécution comportementale |
| 68 | `df` | Exécution comportementale |
| 69 | `df -h` | Exécution comportementale |
| 70 | `lsblk` | Exécution comportementale |
| 71 | `lsblk -f` | Exécution comportementale |
| 72 | `chmod 644 README.md` | Exécution comportementale |
| 73 | `chmod u+x README.md` | Exécution comportementale |
| 74 | `chown ubuntu README.md` | Exécution comportementale |
| 75 | `chgrp ubuntu README.md` | Exécution comportementale |
| 76 | `umask` | Exécution comportementale |
| 77 | `ln -s README.md link.md` | Exécution comportementale |
| 78 | `diff README.md notes.txt` | Exécution comportementale |
| 79 | `sha256sum README.md` | Exécution comportementale |
| 80 | `groups` | Exécution comportementale |
| 81 | `users` | Exécution comportementale |
| 82 | `who` | Exécution comportementale |
| 83 | `w` | Exécution comportementale |
| 84 | `hostname -f` | Exécution comportementale |
| 85 | `hostnamectl` | Exécution comportementale |
| 86 | `uname -r` | Exécution comportementale |
| 87 | `uptime` | Exécution comportementale |
| 88 | `free` | Exécution comportementale |
| 89 | `free -h` | Exécution comportementale |
| 90 | `cal` | Exécution comportementale |
| 91 | `lscpu` | Exécution comportementale |
| 92 | `lsmem` | Exécution comportementale |
| 93 | `ps` | Exécution comportementale |
| 94 | `ps aux` | Exécution comportementale |
| 95 | `ps -ef` | Exécution comportementale |
| 96 | `pgrep bash` | Exécution comportementale |
| 97 | `pidof bash` | Exécution comportementale |
| 98 | `top` | Exécution comportementale |
| 99 | `kill 1234` | Exécution comportementale |
| 100 | `kill -15 1234` | Exécution comportementale |
| 101 | `pkill bash` | Exécution comportementale |
| 102 | `nice -n 10 echo` | Exécution comportementale |
| 103 | `renice 5 -p 1234` | Exécution comportementale |
| 104 | `ip addr` | Exécution comportementale |
| 105 | `ip -br addr` | Exécution comportementale |
| 106 | `ip link` | Exécution comportementale |
| 107 | `ip route` | Exécution comportementale |
| 108 | `ss` | Exécution comportementale |
| 109 | `ss -tulpn` | Exécution comportementale |
| 110 | `ping -c 4 github.com` | Exécution comportementale |
| 111 | `curl https://example.com` | Exécution comportementale |
| 112 | `curl -I https://example.com` | Exécution comportementale |
| 113 | `wget https://example.com/file` | Exécution comportementale |
| 114 | `getent hosts github.com` | Exécution comportementale |
| 115 | `resolvectl status` | Exécution comportementale |
| 116 | `apt --version` | Exécution comportementale |
| 117 | `apt list` | Exécution comportementale |
| 118 | `apt search git` | Exécution comportementale |
| 119 | `apt show git` | Exécution comportementale |
| 120 | `apt update` | Exécution comportementale |
| 121 | `apt install git` | Exécution comportementale |
| 122 | `apt remove git` | Exécution comportementale |
| 123 | `dpkg -l` | Exécution comportementale |
| 124 | `dpkg -s git` | Exécution comportementale |
| 125 | `systemctl status ssh` | Exécution comportementale |
| 126 | `journalctl -u ssh` | Exécution comportementale |
| 127 | `tar -cf archive.tar src` | Exécution comportementale |
| 128 | `tar -tf archive.tar` | Exécution comportementale |
| 129 | `tar -xf archive.tar` | Exécution comportementale |
| 130 | `tar -czf archive.tar.gz src` | Exécution comportementale |
| 131 | `tar -xzf archive.tar.gz` | Exécution comportementale |
| 132 | `gzip notes.txt` | Exécution comportementale |
| 133 | `gunzip notes.txt.gz` | Exécution comportementale |
| 134 | `zip archive.zip README.md` | Exécution comportementale |
| 135 | `unzip archive.zip` | Exécution comportementale |
| 136 | `jobs` | Exécution comportementale |
| 137 | `jobs -l` | Exécution comportementale |
| 138 | `bg` | Exécution comportementale |
| 139 | `fg` | Exécution comportementale |
| 140 | `sleep 1` | Exécution comportementale |
| 141 | `true` | Exécution comportementale |
| 142 | `false` | Exécution comportementale |
| 143 | `md5sum README.md` | Exécution comportementale |
| 144 | `sha1sum README.md` | Exécution comportementale |
| 145 | `sha512sum README.md` | Exécution comportementale |
| 146 | `cksum README.md` | Exécution comportementale |
| 147 | `git --version` | Exécution comportementale |
| 148 | `git init` | Exécution comportementale |
| 149 | `git status` | Exécution comportementale |
| 150 | `git status -s` | Exécution comportementale |
| 151 | `git status --short` | Exécution comportementale |
| 152 | `git status -sb` | Exécution comportementale |
| 153 | `git status --porcelain` | Exécution comportementale |
| 154 | `git add README.md` | Exécution comportementale |
| 155 | `git add .` | Exécution comportementale |
| 156 | `git commit -m "test"` | Exécution comportementale |
| 157 | `git log` | Exécution comportementale |
| 158 | `git log -p` | Exécution comportementale |
| 159 | `git log --oneline` | Exécution comportementale |
| 160 | `git log --stat` | Exécution comportementale |
| 161 | `git log --decorate` | Exécution comportementale |
| 162 | `git log --graph --oneline --decorate --all` | Exécution comportementale |
| 163 | `git diff` | Exécution comportementale |
| 164 | `git diff --staged` | Exécution comportementale |
| 165 | `git diff --cached` | Exécution comportementale |
| 166 | `git branch` | Exécution comportementale |
| 167 | `git branch -a` | Exécution comportementale |
| 168 | `git branch feature2` | Exécution comportementale |
| 169 | `git branch feature2 HEAD` | Exécution comportementale |
| 170 | `git branch -d feature` | Exécution comportementale |
| 171 | `git branch -D feature` | Exécution comportementale |
| 172 | `git branch -m renamed` | Exécution comportementale |
| 173 | `git branch -M main` | Exécution comportementale |
| 174 | `git switch main` | Exécution comportementale |
| 175 | `git switch feature` | Exécution comportementale |
| 176 | `git switch -c feature2` | Exécution comportementale |
| 177 | `git switch -C feature` | Exécution comportementale |
| 178 | `git switch -` | Exécution comportementale |
| 179 | `git checkout main` | Exécution comportementale |
| 180 | `git checkout feature` | Exécution comportementale |
| 181 | `git checkout -b feature2` | Exécution comportementale |
| 182 | `git checkout -B feature` | Exécution comportementale |
| 183 | `git checkout -` | Exécution comportementale |
| 184 | `git checkout -- README.md` | Exécution comportementale |
| 185 | `git checkout HEAD -- README.md` | Exécution comportementale |
| 186 | `git checkout --detach HEAD` | Exécution comportementale |
| 187 | `git restore README.md` | Exécution comportementale |
| 188 | `git restore --staged README.md` | Exécution comportementale |
| 189 | `git restore --source=HEAD README.md` | Exécution comportementale |
| 190 | `git reset README.md` | Exécution comportementale |
| 191 | `git reset HEAD` | Exécution comportementale |
| 192 | `git reset --hard HEAD` | Exécution comportementale |
| 193 | `git reset --soft HEAD~1` | Exécution comportementale |
| 194 | `git reset --mixed HEAD~1` | Exécution comportementale |
| 195 | `git stash` | Exécution comportementale |
| 196 | `git stash push` | Exécution comportementale |
| 197 | `git stash push -m "wip"` | Exécution comportementale |
| 198 | `git stash list` | Exécution comportementale |
| 199 | `git stash apply` | Exécution comportementale |
| 200 | `git stash pop` | Exécution comportementale |
| 201 | `git stash drop` | Exécution comportementale |
| 202 | `git stash clear` | Exécution comportementale |
| 203 | `git reflog` | Exécution comportementale |
| 204 | `git reflog -10` | Exécution comportementale |
| 205 | `git rev-parse HEAD` | Exécution comportementale |
| 206 | `git rev-parse --abbrev-ref HEAD` | Exécution comportementale |
| 207 | `git rev-parse --show-toplevel` | Exécution comportementale |
| 208 | `git rm --cached README.md` | Exécution comportementale |
| 209 | `git mv README.md README2.md` | Exécution comportementale |
| 210 | `git clean -n` | Exécution comportementale |
| 211 | `git clean -nd` | Exécution comportementale |
| 212 | `git clean -fd` | Exécution comportementale |
| 213 | `git remote` | Exécution comportementale |
| 214 | `git remote -v` | Exécution comportementale |
| 215 | `git remote show origin` | Exécution comportementale |
| 216 | `git remote get-url origin` | Exécution comportementale |
| 217 | `git remote add origin https://github.com/example/other.git` | Exécution comportementale |
| 218 | `git remote set-url origin https://github.com/example/other.git` | Exécution comportementale |
| 219 | `git remote rename origin upstream` | Exécution comportementale |
| 220 | `git remote remove origin` | Exécution comportementale |
| 221 | `git fetch` | Exécution comportementale |
| 222 | `git fetch origin` | Exécution comportementale |
| 223 | `git fetch --all` | Exécution comportementale |
| 224 | `git fetch --prune` | Exécution comportementale |
| 225 | `git fetch origin main` | Exécution comportementale |
| 226 | `git pull` | Exécution comportementale |
| 227 | `git pull origin main` | Exécution comportementale |
| 228 | `git pull --rebase origin main` | Exécution comportementale |
| 229 | `git pull --ff-only origin main` | Exécution comportementale |
| 230 | `git push` | Exécution comportementale |
| 231 | `git push origin main` | Exécution comportementale |
| 232 | `git push -u origin main` | Exécution comportementale |
| 233 | `git push --set-upstream origin main` | Exécution comportementale |
| 234 | `git push origin feature` | Exécution comportementale |
| 235 | `git ls-remote origin` | Exécution comportementale |
| 236 | `git ls-remote --heads origin` | Exécution comportementale |
| 237 | `git merge feature` | Exécution comportementale |
| 238 | `git merge --no-ff feature` | Exécution comportementale |
| 239 | `git merge --abort` | Exécution comportementale |
| 240 | `git cherry-pick HEAD` | Exécution comportementale |
| 241 | `git revert HEAD` | Exécution comportementale |
| 242 | `git show --oneline --stat HEAD` | Exécution comportementale |
| 243 | `ls -al ~/.ssh` | Exécution comportementale |
| 244 | `ssh-keygen -t ed25519 -C "test@example.com"` | Exécution comportementale |
| 245 | `eval "$(ssh-agent -s)"` | Exécution comportementale |
| 246 | `ssh-add ~/.ssh/id_ed25519` | Exécution comportementale |
| 247 | `cat ~/.ssh/id_ed25519.pub` | Exécution comportementale |
| 248 | `ssh-keygen -lf ~/.ssh/id_ed25519.pub` | Exécution comportementale |
| 249 | `ssh -T git@github.com` | Exécution comportementale |
| 250 | `help checkout` | Exécution comportementale |

## Critères

- 250/250 commandes doivent passer l'audit prioritaire.
- Le catalogue complet (500+ signatures) est aussi parcouru en smoke test pour détecter les crashes.
- Les commandes du support de cours sont vérifiées par des scénarios stateful dédiés.
- L'APK n'est construit que si les tests réussissent.
