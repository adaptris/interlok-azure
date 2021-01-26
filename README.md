# interlok-azure

[![GitHub tag](https://img.shields.io/github/tag/adaptris/interlok-azure.svg)](https://github.com/adaptris/interlok-azure/tags) [![codecov](https://codecov.io/gh/adaptris/interlok-azure/branch/develop/graph/badge.svg)](https://codecov.io/gh/adaptris/interlok-azure) [![Total alerts](https://img.shields.io/lgtm/alerts/g/adaptris/interlok-azure.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-azure/alerts/) [![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/adaptris/interlok-azure.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/adaptris/interlok-azure/context:java)

The suggested name was `didactic-chainsaw`

## Azure Setup

### Requirements

* Active Office365 subscription
* An Azure Active Directory application with the necessary permissions
  granted.

It is worth remembering the following:

* Daemon applications can work only in Azure AD tenants
* As users cannot interact with daemon applications, incremental
  consent isn't possible

*[See here](https://docs.microsoft.com/en-us/azure/active-directory/develop/scenario-daemon-overview) for an explanation.*

### Application Setup

1. Register an application in the Azure Portal
![Application Registration](docs/o365-1.png)

2. Add a client secret so that the app can identify itself
![Client Secret](docs/o365-2.png)

3. Add the necessary permissions
![Permissions](docs/o365-3.png)

### Dependencies

List of library (JAR) dependencies:

* Microsoft Azure
* Microsoft Graph Core
* Microsoft Graph
* Nimbusds Jose JWT
* Nimbusds oAauth2 OIDC SDK
* Nimbusds Content-Type
* Minidev JSON Smart
* FasterXML Jackson Core
* SquareUp OKHTTP
* SquareUp OKIO
* Google GSON

## Email

Users require an Exchange mailbox to send/receive email, and this
requires an Office365 subscription. The application ID, tenant ID,
client secret and username are all required by Interlok and should match
those given in the Azure portal. When sending mail a list of recipients
is obviously necessary too.

Necessary Azure application permissions:

* Mail.Read
* Mail.ReadBasic
* Mail.ReadBasic.All
* Mail.ReadWrite
* Mail.Send
* User.Read
* User.Read.All

1. Ensure there is a user with an Exchange mailbox
![Users Setup](docs/o365-4.png)

## OneDrive

* Files.Read.All
* Files.ReadWrite.All
* User.Read
* User.Read.All

Many of the prerequisites are the same as for Email: the application ID,
tenant ID, client secret and username are all required by Interlok and
should match those given in the Azure portal.

In addition to a consumer and producer, there are also services for
uploading/downloading documents, if it's necessary during the middle of
a workflow.
