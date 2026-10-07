#!/bin/sh
set -e
export NODE_CONFIG='{
	"server": {
		"siteUrl": "'${DS_URL:-"/"}'",
		"token": {
			"enable": '${JWT_ENABLED:-false}',
			"secret": "'${JWT_SECRET:-developer-only-not-a-real-secret}'",
			"authorizationHeader": "'${JWT_HEADER:-Authorization}'"
		}
	}
}'
exec "$@"
