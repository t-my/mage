# Releasing a new custom build

This fork is **upstream XMage + a small stack of custom commits**. Every release is
made by re-basing that custom stack onto a new upstream release tag and pushing it to
`master`. A push to `master` triggers the **"Build and Publish"** GitHub Actions
workflow ([`.github/workflows/docker-publish.yml`](.github/workflows/docker-publish.yml)),
which builds, publishes the GitHub release, pushes the Docker image, and updates the
GitHub Pages `config.json`. **You do not build or upload the release by hand.**

## TL;DR — cut a release with one click

1. Go to **Actions → "Build and Publish" → Run workflow**.
2. In **upstream_tag**, enter the new upstream tag (e.g. `xmage_1.4.60V1`), or type
   `latest` to auto-pick the newest upstream release.
3. Run it. The workflow fetches the tag, rebases the custom stack onto it, pushes the
   tag + `master`, then builds and publishes the release, Docker image, and Pages config.

That's it. If the rebase hits a merge conflict the run **fails** (you get the GitHub
failure email) — fall back to the [manual procedure](#manual-procedure-fallback--on-conflict)
below for that one release. Leaving **upstream_tag** empty just rebuilds the current
`master` without rebasing.

## How the fork is structured

```
master = <upstream tag xmage_X.Y.ZVn>  +  <N custom commits, applied in order>
```

The custom commits (oldest → newest) are, currently:

1. **Add GHA docker build** — the release pipeline itself (`docker-publish.yml`, `Dockerfile`, `docker/`, `.sdkmanrc`)
2. **Add on-demand card image downloading** — `OnDemandImageDownloader`, `ImageCache`, `PreferencesDialog`
3. **Add auto-refresh for on-demand downloaded card images** — card panel listeners
4. **Add CHANGES.md to track changes** — becomes the release notes body
5. **Fall back to plain text import when a `.dck` has no XMage-formatted cards** — `DeckImporter`
6. **Auto download images by default** — flips the preference default on
7. **Update readme** — custom `readme.md`

The custom commits touch very few upstream files, so they almost always re-apply
cleanly. The only file that overlaps with upstream churn is
`Mage.Client/.../dialog/PreferencesDialog.java` — git usually auto-merges it; if not,
resolve by keeping both upstream's changes and the auto-download checkbox.

## Version naming (automatic)

The workflow derives the version from the nearest upstream tag reachable from `HEAD`:

```
git describe --tags --match 'xmage_*' --abbrev=0 HEAD   →  e.g. xmage_1.4.59V1
```

It then publishes the release as tag **`release-xmage_1.4.59V1`**, titled
**"XMage Custom Build — xmage_1.4.59V1"**, and tags the Docker image
`ghcr.io/t-my/mage:xmage_1.4.59V1`. So the **only** thing that controls the release
name is which upstream tag the new base sits on — nothing to edit by hand.

> ⚠️ `git describe` in CI only sees tags **present on origin (t-my/mage)**. The upstream
> release tag must be pushed to origin (step 1b below) *before* the build runs, otherwise
> the release falls back to the newest `xmage_*` tag origin already has and is mis-named.

## Manual procedure (fallback / on conflict)

Use this only when the one-click run fails on a rebase conflict, or when you want to
run the compile-check locally before publishing.

### Prerequisites (one-time)

- The upstream remote exists: `git remote -v` should show
  `xmage  git@github.com:magefree/mage.git`.
  Add it if missing: `git remote add xmage git@github.com:magefree/mage.git`
- JDK 11 for local compile checks (project builds on JDK 11; see `.sdkmanrc`).

### Step-by-step

Replace `1.4.60V1` with the new upstream release tag, and `1.4.59V1` with the previous
one (the tag the current `master` is based on — check with
`git describe --tags --match 'xmage_*' --abbrev=0 origin/master`).

```bash
cd mage

# 1. Fetch the new upstream release tag
git fetch xmage tag xmage_1.4.60V1

# 1b. CRITICAL: push that tag to origin (t-my/mage). The Build and Publish workflow
#     derives the release name from `git describe --tags --match 'xmage_*'`, and CI only
#     sees tags that exist on origin. A branch push does NOT push tags. If you skip this,
#     the release is mis-named after the newest xmage_* tag origin happens to have.
git push origin xmage_1.4.60V1

# 2. Identify the custom commits currently on master (the stack to carry over).
#    These are everything on master that isn't in the previous upstream base.
PREV=xmage_1.4.59V1            # tag the current master is based on
git log --oneline ${PREV}..origin/master      # should list ONLY the custom commits

# 3. Build the new base = new upstream tag + the custom stack
git checkout -b release/1.4.60V1-base xmage_1.4.60V1
#    Cherry-pick the custom commits in order. <oldest> = the first commit listed by the
#    log above (e.g. "Add GHA docker build"); <newest> = master's HEAD ("Update readme").
git cherry-pick <oldest>^..<newest>
#    e.g.  git cherry-pick e6ff38adf2^..bc9bc6fc4d
#    Resolve any PreferencesDialog.java conflict if it appears, then: git cherry-pick --continue

# 4. Sanity-check
git describe --tags --match 'xmage_*' --abbrev=0 HEAD     # must print xmage_1.4.60V1
git log --oneline xmage_1.4.60V1..HEAD                    # must list exactly the custom commits

# 5. Compile the touched modules against the new upstream APIs (JDK 11)
export JAVA_HOME=~/.sdkman/candidates/java/11.0.25-tem    # or your JDK 11 path
mvn -pl Mage,Mage.Client -am compile -DskipTests -B       # expect BUILD SUCCESS

# 6. (Optional) update CHANGES.md if you added/changed a custom feature.
#    CHANGES.md is pasted verbatim into the release notes.

# 7. Publish: force-update master to the new base. This triggers Build and Publish.
git push origin release/1.4.60V1-base:master --force
```

> The push is a **force-update** because the new base is built on a different upstream
> tag than the old `master` (not a fast-forward). This is expected and is how every
> release is cut. The previous master commit is always recoverable from the reflog or
> the GitHub Actions log if needed.

## After pushing

```bash
# Watch the build
gh run watch "$(gh run list --repo t-my/mage --workflow=docker-publish.yml -L1 --json databaseId -q '.[0].databaseId')" --repo t-my/mage --exit-status

# Verify the release and its asset
gh release view release-xmage_1.4.60V1 --repo t-my/mage
```

A successful run produces:
- GitHub release **`release-xmage_1.4.60V1`** with asset `mage-full_xmage_1.4.60V1_<date>.zip`
- Docker image **`ghcr.io/t-my/mage:xmage_1.4.60V1`** (and `:latest`)
- Updated GitHub Pages `config.json` pointing at the new release

If the build fails, **no release is published** — fix the issue on the branch and
force-push `master` again. `xmage-launcher-qt` picks up the new build automatically via
the Pages `config.json` / the release asset.

## Cleanup (optional)

```bash
git branch -d release/1.4.60V1-base   # the branch is no longer needed; master holds the base
```
