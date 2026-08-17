# Vox

## Contributor Setup



### Environment Variables

Create a local environment file such as `.env` or configure these variables in your IDE run configuration.

Use this format:

```env
VARIABLE_NAME=value
```

Example:

```env
DB_URL=localhost:5432
DB_NAME=vox
DB_USER=postgres
DB_PASSWORD=postgres

SMTP_USERNAME=your-email@example.com
SMTP_PASSWORD=your-smtp-password

JWT_SECRET=replace-with-a-long-secret
PASSWORD_SETUP_URL=http://localhost:3000/setup-password

SYS_ADMIN_EMAIL=admin@example.com
SYS_ADMIN_PASSWORD=change-me
SYS_ADMIN_PHONE=0123456789
SYS_ADMIN_FULLNAME=System Admin
SYS_ADMIN_DATE_OF_BIRTH=1990-01-01
SYS_ADMIN_ADDRESS=Admin address
```

Required variables are referenced from `src/main/resources/application.yaml`.

### Local API UIs

Replace `{PORT}` with the configured server port. The default port is `8080`.

Swagger UI:

```text
http://localhost:{PORT}/swagger-ui/index.html
```

The project also configures this Swagger UI shortcut:

```text
http://localhost:{PORT}/api-doc.html
```

GraphiQL:

```text
http://localhost:{PORT}/graphiql
```
