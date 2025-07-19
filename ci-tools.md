
# Installing yq, AWS CLI, py-pip, and py3-pip on Red Hat

This guide covers installation of `yq`, `aws-cli`, `py-pip`, and `py3-pip` on Red Hat-based systems (e.g., RHEL 8/9, CentOS, AlmaLinux, Rocky Linux).

---

## ✅ 1. Install `yq`

The best way is to use the official GitHub release:

```bash
sudo wget https://github.com/mikefarah/yq/releases/latest/download/yq_linux_amd64 -O /usr/local/bin/yq
sudo chmod +x /usr/local/bin/yq
yq --version
```

> 📝 If you use `dnf install yq`, you may get an old version or a different Go-based tool depending on your distro.

---

## ✅ 2. Install `aws-cli`

Use the official AWS installation method:

```bash
# Download the installer
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"

# Unzip and install
unzip awscliv2.zip
sudo ./aws/install

# Verify
aws --version
```

---

## ✅ 3. Install `py-pip` and `py3-pip`

In RHEL 8/9, `python2` is deprecated. Use Python 3:

```bash
# Enable EPEL if needed (some packages may require it)
sudo dnf install epel-release -y

# Install pip for Python 3
sudo dnf install python3-pip -y

# Check version
pip3 --version
```

If you really need legacy Python 2 `pip`:

```bash
sudo dnf install python2-pip -y
```

> ⚠️ Not recommended unless strictly necessary.

---

## ✅ 4. Optional Cleanup

```bash
rm -rf awscliv2.zip aws/
```

---

Let me know your RHEL version (`cat /etc/redhat-release`) if you need distro-specific tweaks.
