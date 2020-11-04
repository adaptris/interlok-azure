# interlok-azure

[![GitHub tag](https://img.shields.io/github/tag/adaptris/interlok-azure.svg)](https://github.com/adaptris/interlok-azure/tags) [![Build Status](https://travis-ci.com/adaptris/interlok-azure.svg?branch=develop)](https://travis-ci.com/adaptris/interlok-azure) [![CircleCI](https://circleci.com/gh/adaptris/interlok-azure/tree/develop.svg?style=svg)](https://circleci.com/gh/adaptris/interlok-azure/tree/develop) [![codecov](https://codecov.io/gh/adaptris/interlok-azure/branch/develop/graph/badge.svg)](https://codecov.io/gh/adaptris/interlok-azure) [![Total alerts](https://img.shields.io/lgtm/alerts/g/adaptris/interlok-azure.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-azure/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/adaptris/interlok-azure.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-azure/context:java)

The suggested name was `didactic-chainsaw`

## Outlook / Office365

### Requirements

* Active Office365 subscription
* An Azure Active Directory application with application the following
  permissions, and with Admin Consent granted:
  - Mail.Read
  - Mail.ReadBasic
  - Mail.ReadBasic.All
  - Mail.ReadWrite
  - Mail.Send
  - User.Read
  - User.Read.All
* A user to send/receive email

The Office365 consumer and producer require the above because:
* Daemon applications can work only in Azure AD tenants
* As users cannot interact with daemon applications, incremental
  consent isn't possible
* Users require an Exchange mailbox to send/receive email, and this
  requires an Office365 subscription

*[See here](https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-daemon-overview) for an explanation.*

### Application Setup

1. Register an application in the Azure Portal
![Application Registration](docs/o365-1.png)

2. Add a client secret so that the app can identify itself
![Client Secret](docs/o365-2.png)

3. Add the necessary permissions
![Permissions](docs/o365-3.png)

4. Ensure there is a user with an Exchange mailbox
![Users Setup](docs/o365-4.png)

### Interlok Mail Setup

The application ID, tenant ID, client secret and username are all
required and should match those given in the Azure portal. When sending
mail a list of recipients is also required.
