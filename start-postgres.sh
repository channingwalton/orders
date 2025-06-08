#!/bin/bash

# Start PostgreSQL locally in Docker for the orders application

echo "Starting PostgreSQL container for orders application..."

# Check if container already exists
if [ "$(docker ps -aq -f name=orders-postgres)" ]; then
    echo "Container orders-postgres already exists. Stopping and removing..."
    docker stop orders-postgres
    docker rm orders-postgres
fi

# Start PostgreSQL container
docker run -d \
  --name orders-postgres \
  -e POSTGRES_DB=orders \
  -e POSTGRES_USER=orders \
  -e POSTGRES_PASSWORD=orders \
  -p 5432:5432 \
  postgres:15

echo "Waiting for PostgreSQL to be ready..."
sleep 5

# Check if PostgreSQL is ready
until docker exec orders-postgres pg_isready -U orders -d orders; do
    echo "PostgreSQL is not ready yet. Waiting..."
    sleep 2
done

echo "PostgreSQL is ready!"
echo "Database: orders"
echo "User: orders"
echo "Password: orders"
echo "Port: 5432"
echo ""
echo "To connect: psql -h localhost -U orders -d orders"