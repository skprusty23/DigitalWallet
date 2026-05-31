# Git Commit and Push

Commit all pending changes and push.

## What to ask me:
> /git-push

Optionally tell me a commit message. If you don't, I'll write one based on the changed files.

## What I will do:
1. `git status` — see what changed
2. `git diff` — understand the changes  
3. Stage relevant files (skip `.env`, `local.properties`, `*.keystore`)
4. Write a meaningful commit message
5. Commit and push

## Note
The Digital Wallet project currently has **no remote** configured.
To push, first run:
```
git remote add origin <your-github-url>
git push -u origin master
```
Or tell me the remote URL and I'll add it.
