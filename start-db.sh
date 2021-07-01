readonly POSTGRES_CONTAINER_NAME=ppud_postgres

docker ps | grep -q $POSTGRES_CONTAINER_NAME
isPostgresRunning=$?

if [ $isPostgresRunning == 0 ]; then
  echo "Postgres already running"
else
  docker ps -a | grep -q $POSTGRES_CONTAINER_NAME
  isPostgresStopped=$?
  if [ $isPostgresStopped == 0 ]; then
    echo "Starting Postgres..."
    docker start $POSTGRES_CONTAINER_NAME --network=manage-recalls-api
  else
    echo "Creating Postgres..."
    docker pull postgres:13
    docker run --name $POSTGRES_CONTAINER_NAME --network=manage-recalls-api --rm -d -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=ppud_user -e POSTGRES_DB=manage_recalls -p 5432:5432 postgres:13
  fi
fi
