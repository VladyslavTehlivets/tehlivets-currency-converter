# tehlivets-currency-converter-2: base-functionality has no CI

## Symptom
The factory reports base-functionality as red on 41d29df. The run conclusion is `no-ci`.

## Failing run
No run exists. `gh api repos/VladyslavTehlivets/tehlivets-currency-converter/actions/runs?head_sha=41d29df1` returns total_count 0.
No job failed, so no log, step, or billing annotation exists.
The tree of 41d29df has no `.github/workflows` directory.
The only workflow, `ci`, lives on `feat/1-adopt-the-approved-soft-family-r6-design`.
It runs on pull requests there and passed (runs 42753451037, 42759305137).

## Cause
Class: environment gap. This is not a code defect, a flake, an outage, or a billing decline.
The deploy branch has no workflow, so no push to it can start a run.
The ci.yml on the feat/1 branch also expects `./mvnw`, `web/`, and Postgres.
Base-functionality has none of these. It is a plain Maven project (pom.xml, Java sources, JUnit 5).
A copy of that file would fail on this branch.

## Fix
The implementer adds `.github/workflows/ci.yml` on this item branch.
1. Trigger on `pull_request` and on `push` to `base-functionality`.
2. Run one job `java` on ubuntu-latest: checkout, setup-java temurin with maven cache, then `mvn -B verify`.
3. Pick the Java version that builds this pom. It sets source 1.8 and lombok 1.18.6, which fails on JDK 16 and later. Use JDK 11 or raise lombok.
4. Run `mvn -B verify` once locally or in the PR run to prove it is green.
5. Name the cause (no workflow on the deploy branch) in the commit message and include `tehlivets-currency-converter-2`.

The next push to base-functionality after the merge must start a `ci` run that concludes success.
Re-running 41d29df cannot work, because it has no workflow.
Item tehlivets-currency-converter-1 adds a different ci.yml. Its merge into this file needs a manual conflict check.

## Vlad
No question for Vlad. No billing decline exists.
