# 👋🧩 braiNtropy Patches

Custom patches for apps I use, for use with Morphe.

## ❓ About

This repository contains patches maintained by [braiNtropy](https://github.com/braiNtropy).

## 🩹 Patches list

<!-- PATCHES_START EXPANDED -->

<!-- Do not modify this section by hand. The patch list is generated when release.yml creates a new release.
     
     If you wish for the patches list to be collapsed, then remove the word 'EXPANDED' from the comment tag above.

     If you wish to manually keep this list updated then remove the PATCHES_START and PATCHES_END 
     comment blocks entirely. -->

#### A list of your patches will automatically be shown here after your first patches release is created.

&nbsp;

## 🚀 Get started

The local development environment is based on the official Morphe template. To publish it:

1. Create `braiNtropy/braintropy-patches` on GitHub and add it as the `origin` remote.
2. Enable "Allow GitHub Actions to create and approve pull requests" in Settings > Actions > General > Workflow permissions.
3. Add a GitHub credential with `read:packages` access as described in the [Morphe setup guide](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_1_setup.md#-prepare-the-environment).
4. (Optional): Add `patches-bundle.png` to the project if you want a custom icon to show in
   Morphe Manager instead of your GitHub profile avatar.

🎉 You are now ready to start creating patches!

## 🧑‍💻 Usage

To develop and release your Patches using this template:

- Do all development work in the `dev` branch.
- For local development work build your patches using the gradle task `./gradlew buildAndroid` to generate the mpp file found in `patches/build/libs/patches-*.mpp`. Apply your patches locally using Morphe CLI tool like any other patch bundle.
- Always use [Semantic commit](https://kapeli.com/cheat_sheets/Semantic_Commits.docset/Contents/Resources/Documents/index) messages for commits. To keep it simple use only 3 commit message types: `feat: Added a new feature`, `fix: Some problem now fixed`, `chore: Random change you do not want in the user facing changelog`
- Commits of `fix:` and `feat:` will automatically generate new pre-releases and `chore:` will not create a new release.
- Users can apply your dev branch releases by enabling `pre-release` in Morphe Manager patch sources.
- When your dev branch is ready and you want a stable release, merge dev branch to main (do not squash, and only merge).
- **Always use semantic release (release.yml)**. Do not manually upload or creating releases by hand because many files must be updated and release.yml handles everything.

## 🤓 Tips
- See the [patcher documentation](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/1_patcher_intro.md)
  for more examples of creating patches and fingerprints.
- Do not manually edit any generated files such as: `patches-list.json`, `patches-bundle.json`, `CHANGELOG.md`.
  These files will be automatically updated in the release action.
- Do not force push any semantic release commits or you will break the release. To 'redo' the last release then:
  - Git drop the last dev/main semantic release commit you want to redo.
  - Delete the release from the release area of this repo and delete the tag   
  - Make any other changes you wish to do
  - Force push dev/main branch
  - A new replacement release will be created by `release.yml`


<!-- The patches end tag is intentionally placed here so the first release will cleanup 
     this readme of all developer instructions above. -->
<!-- PATCHES_END -->

#### How to use these patches

Click here to add these patches to Morphe: https://morphe.software/add-source?github=braiNtropy/braintropy-patches

Or manually add this repository URL as a patch source in Morphe: https://github.com/braiNtropy/braintropy-patches

### 🛠️ Building

After granting the GitHub CLI token `read:packages` access, build on Windows with:

```powershell
.\build-patches.ps1
```

The patch bundle is generated under `patches/build/libs/`. See the
[Morphe documentation](https://github.com/MorpheApp/morphe-documentation) for the complete workflow.

## 📜 License

braiNtropy Patches are licensed under the [GNU General Public License v3.0](LICENSE).
