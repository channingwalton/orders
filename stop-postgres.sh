#!/bin/bash

# Stop PostgreSQL container for the orders application

echo "Stopping PostgreSQL container..."

if [ "$(docker ps -q -f name=orders-postgres)" ]; then
    docker stop orders-postgres
    echo "PostgreSQL container stopped."
else
    echo "PostgreSQL container is not running."
fi

# Optionally remove the container (uncomment if desired)
# if [ "$(docker ps -aq -f name=orders-postgres)" ]; then
#     docker rm orders-postgres
#     echo "PostgreSQL container removed."
# fi