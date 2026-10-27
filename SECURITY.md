# Security

This is a local observability learning project, not a production deployment.
Its example credentials and disabled security controls are intentionally
simple. Do not expose the services or observability stack to an untrusted
network or reuse these settings outside a disposable local environment.

## Reporting a problem

Please report security problems privately using **[INSERT SECURITY CONTACT
METHOD]**. Include the affected folder/version, steps to reproduce, and any
relevant configuration with secrets removed. Do not publish credentials or
personal data in an issue.

## Insecure local defaults

- MySQL uses the example `root` account and the default password configured
  by the order service unless overridden.
- Grafana is provisioned with `admin` / `admin`.
- Loki authentication is disabled in the v4 local stack.
- OpenSearch security is disabled and it uses plain HTTP in the v5 local stack.
- Prometheus, Tempo, and the Collector have no authentication configured in
  these local Compose stacks.

Override or replace these settings before any use beyond local learning.
