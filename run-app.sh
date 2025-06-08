#!/bin/bash

# Run the orders application

echo "Starting orders application..."

# Check if PostgreSQL container is running
if ! docker ps | grep -q orders-postgres; then
    echo "PostgreSQL container is not running. Starting it first..."
    ./start-postgres.sh
    echo ""
fi

echo "Compiling and running the application..."
sbt run