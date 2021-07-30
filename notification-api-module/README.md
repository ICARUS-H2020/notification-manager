# ICARUS Notification API Module
## Overview
The ICARUS Notification API Module is a containerized service that provides the ability to access, upate or delete the notifications that are stored in the database.

## Install
The whole service runs in the container, and the user needs first to compile the maven project and then run the appropriate docker commands.

### Maven build
```
mvn clean install package -Denvironment=prod -DskipTests
```

### Docker build
```
docker build -t notification_api_module .
```

### Docker run
```
docker run -d -p 8088:8088 --rm --name notification_api_module notification_api_module
```

### Docker stop
```
docker stop notification_api_module
```

## Available REST API Requests
Each request requires a token that will verify if (i) the user is verified to get notifications and (ii) to be able to get only the notifications of that user.
### GET Requests

#### Get all notifications
```
localhost:8088/api/v1/notifications/
```

#### Get all unseen notifications
```
localhost:8088/api/v1/notifications/unseen
```

#### Get all seen notifications
```
localhost:8088/api/v1/notifications/seen
```

#### Get a specific notification
```
localhost:8088/api/v1/notifications/{nid}
```
### PUT Requests
#### Update all notifications as "seen"
```
localhost:8088/api/v1/notifications/seen
```
#### Update all notifications as "unseen"
```
localhost:8088/api/v1/notifications/unseen
```
#### Update a specific notification as "seen"
```
localhost:8088/api/v1/notifications/{nid}/seen
```
#### Update a specific notification as "unseen"
```
localhost:8088/api/v1/notifications/{nid}/unseen
```
### Delete Requests
#### Delete a specific notification
```
localhost:8088/api/v1/notifications/{nid}
```