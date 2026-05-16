# 🏠 CAT AI Assistant — Scrum Workspace

> This is your AI-powered Scrum workspace. Two developers + AI team members (Architect, PO, Scrum Master) working together.

---

## 📂 Folder Structure

```
scrum-workspace/
├── WORKSPACE_README.md        ← You are here
├── TEAM.md                    ← Team members & roles
├── SCRUM_BOARD.md             ← Live sprint board (To Do / In Progress / Done)
│
├── roles/                     ← AI role instruction files
│   ├── ARCHITECT.md           ← Start a chat with this for architecture decisions
│   ├── PRODUCT_OWNER.md       ← Start a chat with this for stories & backlog
│   ├── SCRUM_MASTER.md        ← Start a chat with this for sprint management
│   └── DEVELOPER.md           ← Start a chat with this for coding help
│
├── backlog/
│   └── PRODUCT_BACKLOG.md     ← All user stories, prioritized
│
└── sprints/
    └── SPRINT_1.md            ← Current sprint plan
```

---

## 🤖 How to Use the AI Roles

Each file in `roles/` is an **instruction file**. When you start a new Claude chat for a specific purpose:

### Step 1 — Open a new chat
### Step 2 — Attach the role file + relevant context
### Step 3 — Start working

| When you need to...                        | Attach this role file     | Also attach...                      |
|--------------------------------------------|---------------------------|-------------------------------------|
| Make architecture decisions                | `roles/ARCHITECT.md`      | `projectStart.md`                   |
| Create/refine user stories                 | `roles/PRODUCT_OWNER.md`  | `backlog/PRODUCT_BACKLOG.md`        |
| Plan sprints, track progress               | `roles/SCRUM_MASTER.md`   | `SCRUM_BOARD.md` + current sprint   |
| Write code for a story                     | `roles/DEVELOPER.md`      | `projectStart.md` + the story       |

### Example prompts:

**To Architect:**
> "Here's my architect instructions and project notes. Should we use WebFlux or stick with Spring MVC for our streaming endpoint?"

**To Product Owner:**
> "Here's my PO instructions and backlog. Write acceptance criteria for the data ingestion story."

**To Scrum Master:**
> "Here's my SM instructions and board. We finished US-001 and US-002. Update the board and suggest what to pick next."

**To Developer:**
> "Here's my dev instructions and project notes. Implement story US-003: Integrate Claude API via Spring AI."

---

## 📋 Daily Workflow

### Morning (Standup)
1. Open chat with **Scrum Master** role
2. Attach `SCRUM_BOARD.md` + current sprint file
3. Say: *"Run standup. Here's our board. I completed X yesterday, working on Y today."*
4. SM will update the board and flag blockers

### During the Day (Coding)
1. Pick a story from `SCRUM_BOARD.md`
2. Move it to "In Progress" 
3. Open chat with **Developer** role
4. Attach `projectStart.md` + the story details
5. Code together

### When Stuck (Architecture)
1. Open chat with **Architect** role
2. Ask your technical question
3. Architect will reference the tech stack and project patterns

### End of Sprint
1. Open chat with **Scrum Master** role
2. Say: *"Run sprint retrospective for Sprint 1"*
3. SM will summarize what was done, velocity, and plan Sprint 2

---

## 👥 Team

| Member   | Role          |
|----------|---------------|
| vagrover | Developer 1   |
| parora   | Developer 2   |
| Claude   | Architect + PO + SM + Dev Assistant |

See `TEAM.md` for full details.

---

## 📜 Rules

1. **One role per chat** — Don't mix Architect and Developer in the same conversation
2. **Always attach context** — The AI roles work best with the relevant files attached
3. **Update the board** — After completing work, update `SCRUM_BOARD.md`
4. **Stories before code** — Check the backlog before starting new work
5. **Ask the Architect first** — For any tech decisions not covered in `projectStart.md`

