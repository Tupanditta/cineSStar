## Git Author Identity — Mandatory

The Git author and committer identity must always belong to the repository owner, never to Claude.

### Rules

1. NEVER configure Claude as the Git author or committer.
2. NEVER change `git config user.name` or `git config user.email`.
3. NEVER use Claude's name, email, or bot identity in Git commits.
4. When generating code, do not create commits automatically.
5. I am responsible for creating and publishing all commits.
6. Do not modify existing commit authors or rewrite Git history without explicit permission.

### Commit identity verification

Before any commit operation (only when explicitly authorized), verify:

- `git config user.name`
- `git config user.email`

The author and committer must use my configured GitHub identity.

If the identity is not mine or cannot be verified, STOP and ask me before proceeding.

### Important

Claude is a coding assistant, not a repository contributor.

All commits must be attributed to the repository owner, who is solely responsible for committing and pushing changes.