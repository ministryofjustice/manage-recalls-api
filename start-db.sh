docker pull postgres:13

docker run --name ppud_postgres -d -e POSTGRES_PASSWORD=secret -e POSTGRES_USER=ppud_user -e POSTGRES_DB=manage_recalls -p 5432:5432 postgres:13