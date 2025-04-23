#!/bin/bash

# Build for multiple platforms
docker buildx create --use
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t your-registry/jwt-grpc-server:latest \
  -t your-registry/jwt-grpc-server:$(git rev-parse --short HEAD) \
  --push .