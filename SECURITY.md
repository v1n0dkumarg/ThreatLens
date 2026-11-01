# Security Policy

## Scope

ThreatLens is a defensive local portfolio system using reserved example indicators and synthetic sightings. It does not poll external feeds or contact submitted observables. Do not send real credentials, production indicators, or personal data to it.

## Reporting

Do not publish secrets or sensitive data in a public issue. Use GitHub private vulnerability reporting when available. Include the affected version, local reproduction steps, and impact.

## Deployment

The checked-in users and passwords are local examples. The app binds to loopback in Compose and uses HTTP Basic for local testing. Before any non-local deployment, replace demo users with OIDC-backed identity, require TLS, use managed secret storage, and review audit and network controls. Treat all imported intelligence and event content as untrusted input.
