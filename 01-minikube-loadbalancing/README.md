# 🚀 Week 1: Minikube + Podman 로드밸런싱

## 🎯 학습 목표
- Podman과 Minikube의 개념 및 구조 이해
- Nginx v1/v2 버전 로드밸런싱 실습
- Kubernetes 리소스(ConfigMap, Deployment, Service) 동작 원리 실습

## ✅ Podman
루트 권한 없이 쓸 수 있는 Docker 대체 도구.

Podman은 컨테이너 실행, 빌드, 관리하는 CLI 기반의 컨테이너 엔진으로,
Docker와 거의 동일한 명령어를 사용하지만 보안성과 Kubernetes 호환성을 강화한 것이 특징

Docker와 거의 동일한 명령어 사용 가능 ( podman pull, podman run, podman ps, podman build, …)

| 항목 | Podman | Docker |
| --- | --- | --- |
| **데몬(daemon)** | 없음 (daemonless) | dockerd 데몬 필요 |
| **루트 권한 필요 여부** | 불필요 (rootless mode) | 기본적으로 root 필요 |
| **Pod 개념** | 있음 (Kubernetes 호환) | 없음 (컨테이너 단위) |

### Podman Machine (가상머신)
macOS/Windows에서는 Podman이 직접 리눅스 커널을 실행할 수 없기 때문에
“Podman Machine” 이라는 경량 리눅스 VM 위에서 컨테이너를 실행

```bash
# 가상머신 생성
podman machine init --cpus 2 --memory 2048 --disk-size 20 --rootful

# 가상머신 시작
podman machine start

# 상태확인
podman machine list
podman info
```
→ Podman Machine은 리눅스 환경을 에뮬레이션하는 VM으로 이 위에서 Podman 컨테이너가 구동됨

## ✅ Minikube
로컬 컴퓨터에서 쿠버네티스를 간단히 실행할 수 있게 해주는 경량 클러스터 도구
- 1개의 노드: Control Plane + Worker 역할을 모두 포함
- 실행 환경: Docker, Podman, VirtualBox 등 다양한 드라이버 위에서 실행

🔸 설치
```bash
# 최신 바이너리를 github에서 다운로드
curl -LO https://github.com/kubernetes/minikube/releases/latest/download/minikube-darwin-arm64

# minikube 명령을 전역명령어 PATH로 등록
sudo install minikube-darwin-arm64 /usr/local/bin/minikube
```

🔸 Minikube + Podman 연동 구조
```bash
minikube start --driver=podman --container-runtime=cri-o
```
- driver = podman → Podman Machine 위에 Kubernetes 클러스터 생성
- container-runtime = cri-o → Podman과 가장 잘 호환되는 컨테이너 런타임

🔸 실행순서
```bash
podman machine init --cpus 2 --memory 2048 --disk-size 20 --rootful
podman machine start
minikube start --driver=podman --container-runtime=cri-o
```

```bash
macOS
 └── Podman Machine (리눅스 VM)
      └── Minikube 클러스터
           └── Control-plane 노드 (Podman 위 컨테이너)
                ├─ kube-apiserver
                ├─ etcd
                ├─ kube-scheduler
                └─ CRI-O (컨테이너 런타임)

```
- Podman Machine의 리소스 스펙(CPU, RAM, Disk)이 Minikube 노드의 실제 리소스 스펙이됨
- ~/.kube/config에 자동 등록되어, kubectl이 이 클러스터를 기본으로 사용

## 🧩 Nginx Load Balancing 실습
(1) 버전별 Nginx 설정파일 준비 (/manifests/v1.conf, /manifests/v2.conf)

루트경로(/)에서 서로다른 버전정보("v1", "v2")를 리턴하도록 지정한다.

(2) ConfigMap 생성
```bash
kubectl create configmap nginx-v1-config --from-file=v1.conf
kubectl create configmap nginx-v2-config --from-file=v2.conf
```

(3) 버전별 deployment를 2개 생성 (/mainfests/nginx-v1-deployment.yaml, /mainfests/nginx-v2-deployment.yaml)
- 공통 라벨: app=nginx
- 버전 라벨: version=v1, version=v2
- ConfigMap을 /etc/nginx/conf.d(nignx컨테이너의 설정파일경로)에 볼륨으로 마운트한다( = 외부에서 설정내용을 주입 )

(4) Service 생성 (/manifests/nginx-service.yaml)
- Selector: app=nginx
- 두 Deployment를 하나의 서비스로 연결 (로드밸런싱)

(5) Minikube 서비스 접근
```bash
minikube service nginx-service
```
- Minikube가 클러스터 내부 포트(30001) → 로컬 포트로 포워딩
- 브라우저 자동 실행 또는 curl 로 테스트 가능
→ 요청 시 v1, v2 응답이 번갈아 표시됨

(6) Replica 조절 테스트
```bash
kubectl scale deployment nginx-v1 --replicas=5
```
→ v1 Pod 개수를 늘리면 v1 응답 비율이 증가

## 🧠 결과 요약
- `minikube service nginx-service` 실행 시 v1/v2 번갈아 응답
- replica 조정에 따라 응답 비율 변화 확인

## 회고
- Podman Machine의 리소스가 Minikube 노드에 직접 반영됨을 이해
- Kubernetes의 Service → Pod 라우팅 구조 체험
- ConfigMap을 이용한 Nginx 설정 주입 과정을 통해 Kubernetes 설정관리 철학 파악
- Replica 조절로 실제 로드밸런싱 효과 확인
