#!/bin/bash

# Apply Kubernetes configuration
kubectl apply -f deployment.yaml

# Wait for rollout to complete
kubectl rollout status deployment/auth-service