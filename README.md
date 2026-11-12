# Task Manager REST API

A starter REST API using Java 17, Spring Boot 4.1.2, Spring Security, JWT, Spring Data JPA and MySQL 8.4.

## Features

- User registration and login
- JWT bearer-token authentication
- BCrypt password hashing
- User-owned task CRUD (users cannot access another user's tasks)
- Bean Validation and JSON error responses
- Configurable CORS
- Health endpoint: `GET /api/health`

## Requirements

- JDK 17
- Maven 3.6.3+
- Docker Desktop / Docker Engine (for local MySQL), or a local MySQL 8 server

## 1. Run locally

Start MySQL:

```bash
docker compose up -d
```

Generate a JWT secret:

```bash
export JWT_SECRET="$(openssl rand -base64 32)"
```

Set local database variables (these match `docker-compose.yml`):

```bash
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=task_manager
export DB_USERNAME=taskapp
export DB_PASSWORD=local-task-password
export CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Build and run:

```bash
mvn clean package
java -jar target/task-manager-api-0.0.1-SNAPSHOT.jar
```

Health check:

```bash
curl http://localhost:8080/api/health
```

## 2. API reference

Base URL locally: `http://localhost:8080`

| Method | Endpoint | Auth | Purpose |
|---|---|---|---|
| GET | `/api/health` | No | Health check |
| POST | `/api/auth/register` | No | Create an account |
| POST | `/api/auth/login` | No | Login and receive JWT |
| GET | `/api/tasks` | Bearer JWT | List your tasks |
| POST | `/api/tasks` | Bearer JWT | Create a task |
| PUT | `/api/tasks/{id}` | Bearer JWT | Update your task |
| DELETE | `/api/tasks/{id}` | Bearer JWT | Delete your task |

### Register

```bash
curl -i -X POST http://localhost:8080/api/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"name":"Demo User","email":"demo@example.com","password":"StrongPass123!"}'
```

Response contains `token`, `tokenType`, `userId`, `name`, and `email`. Save the token.

### Login

```bash
curl -i -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"demo@example.com","password":"StrongPass123!"}'
```

### Create a task

Replace `YOUR_JWT` with the token returned by register/login:

```bash
curl -i -X POST http://localhost:8080/api/tasks \
  -H 'Authorization: Bearer YOUR_JWT' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Deploy Spring Boot to AWS","description":"Create EC2 deployment","completed":false}'
```

### List tasks

```bash
curl -i http://localhost:8080/api/tasks \
  -H 'Authorization: Bearer YOUR_JWT'
```

### Update a task

```bash
curl -i -X PUT http://localhost:8080/api/tasks/1 \
  -H 'Authorization: Bearer YOUR_JWT' \
  -H 'Content-Type: application/json' \
  -d '{"title":"Deployment complete","description":"API is live","completed":true}'
```

### Delete a task

```bash
curl -i -X DELETE http://localhost:8080/api/tasks/1 \
  -H 'Authorization: Bearer YOUR_JWT'
```

## 3. Create a GitHub repository

Create an empty repository named `task-manager-api` on GitHub. Do not initialize it with a README if you already have this project README locally.

From this project directory:

```bash
git init
git add .
git commit -m "Initial Spring Boot task manager API"
git branch -M main
git remote add origin https://github.com/YOUR_USERNAME/task-manager-api.git
git push -u origin main
```

The included GitHub Actions workflow builds and tests the project on pushes and pull requests to `main`.

## 4. AWS cost-control plan

This guide targets a new AWS account using the AWS Free account plan where available. AWS's current new-customer offer is credit-based (up to USD 200, subject to eligibility and terms), not the old blanket 12-month free EC2/RDS promise. The Free account plan ends after six months or when credits are exhausted, whichever comes first. Confirm the current plan and service availability in your account before creating resources.

To minimize spend for this learning deployment:

- Start with one EC2 instance only, if EC2 is available under your account's Free plan.
- Install MySQL on the same EC2 instance; do not create RDS yet.
- Do not create an Application Load Balancer, NAT Gateway, Elastic IP, RDS, or paid support plan.
- Use GitHub Pages for the React frontend.
- Do not create snapshots or extra EBS volumes unless required.
- Stop or terminate the EC2 instance when you are not using it. EBS storage and some other resources can still incur costs while an instance is stopped.
- Check Billing > Credits and Free Tier usage regularly. On the Free plan, resources and access can be restricted when the plan/credits end; don't rely on credits as a permanent free host.

Official current details:
- https://aws.amazon.com/free/free-tier-faqs/
- https://docs.aws.amazon.com/awsaccountbilling/latest/aboutv2/free-tier-plans.html

## 5. Create your AWS account safely

1. Go to https://aws.amazon.com/free/.
2. Create a new account using your own email and account details.
3. Choose the **Free account plan** if offered and appropriate for your learning project. Do not choose a paid plan merely to follow this guide.
4. Complete identity verification.
5. Enable MFA on the root user.
6. Do not create root access keys.
7. Open Billing / Free Tier and verify credits, plan end date, and which EC2 offers are available.
8. If EC2 is not available on your Free plan, stop here rather than launching a paid resource. The account plan and current offer catalog determine availability.

## 6. Launch one EC2 instance

In the AWS Console, choose the region closest to your users (for example, Asia Pacific Mumbai `ap-south-1`), then open EC2.

Launch an Ubuntu Server 24.04 LTS instance using only an instance type that the EC2 launch wizard explicitly identifies as eligible/available under your account's current Free plan. Do not assume a particular instance type is free.

Suggested learning configuration:
- One instance
- Ubuntu Server 24.04 LTS
- Smallest suitable eligible instance type
- 16–20 GiB root EBS volume if the console permits and it is sufficient
- One security group
- No load balancer, NAT Gateway, RDS, or Elastic IP

Create/download an SSH key pair (`.pem`) and store it securely.

### Security group

For an initial HTTP-only test:
- SSH TCP 22: source = your current public IP only
- HTTP TCP 80: source = `0.0.0.0/0`
- Do not add inbound 3306 (MySQL)
- Do not expose 8080 publicly; Nginx will proxy to localhost

If you later enable HTTPS, add TCP 443 from `0.0.0.0/0`.

## 7. Connect to EC2 from Ubuntu

On your local Linux machine:

```bash
chmod 400 task-manager-ec2.pem
ssh -i task-manager-ec2.pem ubuntu@EC2_PUBLIC_IPV4
```

Update and install Java, MySQL and Nginx:

```bash
sudo apt update
sudo apt upgrade -y
sudo apt install -y openjdk-17-jre-headless mysql-server nginx
java -version
mysql --version
```

## 8. Configure MySQL on EC2

MySQL should listen only on loopback. Check `/etc/mysql/mysql.conf.d/mysqld.cnf` and set:

```ini
bind-address = 127.0.0.1
```

Restart:

```bash
sudo systemctl enable mysql
sudo systemctl restart mysql
```

Create the application database and local-only database account:

```bash
sudo mysql
```

At the MySQL prompt, use a strong unique password in place of the placeholder:

```sql
CREATE DATABASE task_manager CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'taskapp'@'localhost' IDENTIFIED BY 'REPLACE_WITH_A_LONG_RANDOM_PASSWORD';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES
ON task_manager.* TO 'taskapp'@'localhost';
FLUSH PRIVILEGES;
EXIT;
```

Do not reuse your AWS password. Do not open MySQL port 3306 in the EC2 security group.

## 9. Build and upload the Spring Boot JAR

On your local development machine, install JDK 17 and Maven, then:

```bash
mvn clean verify
```

Upload the JAR:

```bash
scp -i task-manager-ec2.pem \
  target/task-manager-api-0.0.1-SNAPSHOT.jar \
  ubuntu@EC2_PUBLIC_IPV4:/home/ubuntu/task-manager-api.jar
```

## 10. Configure production environment variables

On EC2, create an environment file:

```bash
sudo install -d -m 750 /etc/task-manager
sudo nano /etc/task-manager/task-manager.env
```

Add values (replace every placeholder):

```bash
SERVER_PORT=8080
DB_HOST=localhost
DB_PORT=3306
DB_NAME=task_manager
DB_USERNAME=taskapp
DB_PASSWORD=REPLACE_WITH_MYSQL_PASSWORD
JWT_SECRET=REPLACE_WITH_BASE64_SECRET
JWT_EXPIRATION_MINUTES=60
CORS_ALLOWED_ORIGINS=https://YOUR_USERNAME.github.io,https://YOUR_CUSTOM_FRONTEND_DOMAIN
DDL_AUTO=update
```

Generate a Base64 JWT secret on your local machine:

```bash
openssl rand -base64 32
```

Copy the output into `JWT_SECRET`. Do not commit the environment file.

Secure it:

```bash
sudo chown root:root /etc/task-manager/task-manager.env
sudo chmod 600 /etc/task-manager/task-manager.env
```

`DDL_AUTO=update` is convenient for this starter project. Before a real production workload, use Flyway or Liquibase migrations and change this to `validate`.

## 11. Run Spring Boot as a systemd service

Create the service:

```bash
sudo nano /etc/systemd/system/task-manager.service
```

Paste:

```ini
[Unit]
Description=Task Manager Spring Boot API
After=network.target mysql.service
Requires=mysql.service

[Service]
Type=simple
User=ubuntu
EnvironmentFile=/etc/task-manager/task-manager.env
WorkingDirectory=/home/ubuntu
ExecStart=/usr/bin/java -Xms128m -Xmx384m -jar /home/ubuntu/task-manager-api.jar
SuccessExitStatus=143
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable task-manager
sudo systemctl start task-manager
sudo systemctl status task-manager
```

View logs:

```bash
sudo journalctl -u task-manager -f
```

Test locally on EC2:

```bash
curl http://127.0.0.1:8080/api/health
```

Expected JSON includes `"status":"UP"`.

If the process is killed due to memory pressure, reduce the JVM heap or choose a larger eligible instance; do not blindly increase heap on a small VM.

## 12. Configure Nginx reverse proxy

Create:

```bash
sudo nano /etc/nginx/sites-available/task-manager
```

Add:

```nginx
server {
    listen 80;
    server_name _;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

Enable it:

```bash
sudo ln -s /etc/nginx/sites-available/task-manager /etc/nginx/sites-enabled/task-manager
sudo nginx -t
sudo systemctl reload nginx
```

Test from your local computer:

```bash
curl http://EC2_PUBLIC_IPV4/api/health
```

Do not use HTTP for real credentials or JWTs over an untrusted network. The next section explains HTTPS.

## 13. Optional HTTPS (requires a domain)

A custom domain is not free in general. If you already own a domain, point an `A` record such as `api.yourdomain.com` to the EC2 public IPv4 address. Without an Elastic IP, the public IPv4 can change when the instance is stopped and started; update DNS if it changes.

Update the Nginx `server_name` to `api.yourdomain.com`, then install Certbot:

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d api.yourdomain.com
```

Follow the prompts and choose the HTTPS redirect option. Test renewal:

```bash
sudo certbot renew --dry-run
```

Let's Encrypt certificates are free, but domain registration and any AWS networking resources are not necessarily free. Avoid creating an Elastic IP without checking its current pricing and Free Tier eligibility.

If you don't own a domain, use the EC2 public IPv4 only for temporary testing; do not send login credentials or bearer tokens over plain HTTP.

## 14. Connect GitHub Pages React frontend

For Vite, define the production API base URL in the frontend build environment:

```env
VITE_API_BASE_URL=https://api.yourdomain.com
```

Then call the API with:

```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL;

const response = await fetch(`${API_BASE_URL}/api/tasks`, {
  headers: {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json"
  }
});
```

If you use GitHub Pages' default URL, set `CORS_ALLOWED_ORIGINS` to the exact origin, for example:

```text
https://YOUR_USERNAME.github.io
```

For a project Pages URL, the origin is still `https://YOUR_USERNAME.github.io` (the repository path is not part of the origin). If you use a custom domain, include that exact origin too.

After changing the EC2 environment file:

```bash
sudo systemctl restart task-manager
```

## 15. Deploying an update

Build locally:

```bash
mvn clean verify
```

Upload:

```bash
scp -i task-manager-ec2.pem \
  target/task-manager-api-0.0.1-SNAPSHOT.jar \
  ubuntu@EC2_PUBLIC_IPV4:/home/ubuntu/task-manager-api.jar
```

Restart:

```bash
ssh -i task-manager-ec2.pem ubuntu@EC2_PUBLIC_IPV4 \
  'sudo systemctl restart task-manager && sudo systemctl status task-manager --no-pager'
```

For a safer deployment, upload to a versioned filename, retain the previous JAR, then switch a symlink or service path and verify the health endpoint before removing the old version.

## 16. AWS cost safety checklist

Before leaving the AWS console:
- Confirm the account is on the Free account plan, if that is the intended plan.
- Check the account's Free Tier/credits page and verify the EC2 offer applies.
- Do not create RDS, NAT Gateway, ALB, or paid support for this first deployment.
- Do not create an Elastic IP without checking its price.
- Check EBS volume size and snapshots.
- Monitor Free Tier credits and account plan expiration.
- Terminate the EC2 instance and delete its EBS volume when you are finished experimenting and no longer need the data. Export a database backup first if you want to keep it.
- Remember that a stopped EC2 instance can still incur EBS storage charges on a paid account.

## 17. Important production hardening before real users

This is a functional starter, not a fully audited production security implementation. Before public production use:
- Add rate limiting and login abuse protection.
- Add email verification and password reset.
- Add refresh-token rotation/revocation if required.
- Use HTTPS only.
- Add database migrations (Flyway/Liquibase), backups and restore testing.
- Restrict CORS to exact frontend origins.
- Consider storing tokens in secure, HttpOnly cookies instead of browser local storage; if so, implement CSRF protections.
- Keep dependencies patched and add integration/security tests.
- Add request logging without recording passwords or bearer tokens.
- Consider managed database hosting and multi-AZ architecture when budget permits.
