# Deployment Notes

This helm deployment makes use of the generic-service chart, see chart documentation here:

<https://github.com/ministryofjustice/hmpps-helm-charts/tree/main/charts/generic-service>

## Kubernetes Secrets

This repo uses `git-crypt` to encrypt the `manage-recalls-api/templates/secrets.yaml` file...

To decrypt the files locally (if you are an authorised user):

```
git-crypt unlock
```

To add a user (as an authorised user), first, unlock the repo (see above) create a new branch, then run the following command:

```
git-crypt add-gpg-user USER_ID
```

> USER_ID can be a key ID, a full fingerprint, an email address, or anything else that uniquely identifies a public key to GPG (see "HOW TO SPECIFY A USER ID" in the gpg man page). Note: git-crypt add-gpg-user will add and commit a GPG-encrypted key file in the .git-crypt directory of the root of your repository.

Then push the automatically added commit to the branch and create a pull-request.
