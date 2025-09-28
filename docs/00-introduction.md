# Introduction

`awe4lb` is a lightweight, high-performance Layer 4 (TCP/UDP) load balancer designed to address specific gaps in existing solutions while maintaining simplicity and extensibility, offering a minimal yet powerful alternative to heavyweight proxies.

`awe4lb` uses a concise JSON-based DSL, making it easy to deploy and manage.

## Targeted Use Cases

Optimized for TCP/UDP proxying, TLS termination, and dynamic discovery (e.g., Kubernetes, HTTP, exec), it excels in environments needing low-overhead solutions for APIs, databases, or custom protocols.

## Minimalism

Built to handle production workloads with fewer resources than NGINX or Traefik, it’s ideal for constrained environments (e.g., edge devices or small clusters).

It supports custom discovery and health checks, with a REST API and web UI for runtime management.