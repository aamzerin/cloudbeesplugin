# **Install EFS CSI Driver on Amazon EKS Without Helm**

## **1. Prerequisites**
- A working **Amazon EKS** cluster.
- An **Amazon EFS** file system.
- **kubectl** installed and configured.
- AWS CLI installed and configured.

---

## **2. Create an IAM Policy for the EFS CSI Driver**
Create a JSON file for the IAM policy:

```sh
cat <<EOF > efs-csi-policy.json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "elasticfilesystem:DescribeFileSystems",
                "elasticfilesystem:DescribeMountTargets",
                "elasticfilesystem:DescribeAccessPoints",
                "elasticfilesystem:CreateAccessPoint",
                "elasticfilesystem:DeleteAccessPoint"
            ],
            "Resource": "*"
        },
        {
            "Effect": "Allow",
            "Action": "elasticfilesystem:ClientMount",
            "Resource": "arn:aws:elasticfilesystem:<region>:<account-id>:file-system/*"
        },
        {
            "Effect": "Allow",
            "Action": "elasticfilesystem:ClientWrite",
            "Resource": "arn:aws:elasticfilesystem:<region>:<account-id>:file-system/*"
        }
    ]
}
EOF
```

Replace **`<region>`** and **`<account-id>`** with your actual AWS region and account ID.

Create the IAM policy:

```sh
aws iam create-policy --policy-name AmazonEKS_EFS_CSI_Driver_Policy --policy-document file://efs-csi-policy.json
```

---

## **3. Create an IAM Role and Associate it with a Kubernetes ServiceAccount**
```sh
eksctl create iamserviceaccount   --name efs-csi-controller-sa   --namespace kube-system   --cluster <cluster-name>   --attach-policy-arn arn:aws:iam::<account-id>:policy/AmazonEKS_EFS_CSI_Driver_Policy   --approve
```

Replace **`<cluster-name>`** and **`<account-id>`** with your actual values.

---

## **4. Deploy the EFS CSI Driver (Manually)**
Apply the **CSI Driver deployment YAML** files.

### **4.1 Deploy RBAC (Roles & Service Account)**
```sh
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-efs-csi-driver/master/deploy/kubernetes/rbac.yaml
```

### **4.2 Deploy the CSI Driver**
```sh
kubectl apply -f https://raw.githubusercontent.com/kubernetes-sigs/aws-efs-csi-driver/master/deploy/kubernetes/efs-csi-driver.yaml
```

---

## **5. Create a StorageClass for EFS**
Manually define a `StorageClass` for EFS:

```sh
cat <<EOF | kubectl apply -f -
apiVersion: storage.k8s.io/v1
kind: StorageClass
metadata:
  name: efs-sc
provisioner: efs.csi.aws.com
EOF
```

---

## **6. Create a PersistentVolume (PV)**
```sh
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolume
metadata:
  name: efs-pv
spec:
  capacity:
    storage: 5Gi
  volumeMode: Filesystem
  accessModes:
    - ReadWriteMany
  persistentVolumeReclaimPolicy: Retain
  storageClassName: efs-sc
  csi:
    driver: efs.csi.aws.com
    volumeHandle: <file-system-id>
EOF
```

Replace **`<file-system-id>`** with your Amazon EFS file system ID.

---

## **7. Create a PersistentVolumeClaim (PVC)**
```sh
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: efs-pvc
spec:
  accessModes:
    - ReadWriteMany
  storageClassName: efs-sc
  resources:
    requests:
      storage: 5Gi
EOF
```

---

## **8. Deploy a Test Pod to Verify the Setup**
```sh
cat <<EOF | kubectl apply -f -
apiVersion: v1
kind: Pod
metadata:
  name: efs-app
spec:
  containers:
    - name: app
      image: nginx
      volumeMounts:
        - mountPath: "/data"
          name: efs-storage
  volumes:
    - name: efs-storage
      persistentVolumeClaim:
        claimName: efs-pvc
EOF
```

---

## **9. Verify the Installation**
Check if the EFS CSI driver pods are running:

```sh
kubectl get pods -n kube-system | grep efs
```

Check if the **StorageClass**, PV, and PVC are created:

```sh
kubectl get storageclass
kubectl get pv
kubectl get pvc
```

Check if the Pod is running and can write to EFS:

```sh
kubectl exec -it efs-app -- sh
```

Inside the container, run:

```sh
echo "EFS is working" > /data/testfile
cat /data/testfile
```

---

## **Conclusion**
You have successfully installed the **EFS CSI Driver on Amazon EKS without Helm**. The **EFS storage** is now accessible by your Kubernetes workloads. Let me know if you need help troubleshooting!
