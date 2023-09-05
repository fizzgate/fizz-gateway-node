#!/bin/bash
# ----------------------------------------------------------------------
# use : wget https://gitee.com/fizzgate/fizz-gateway-node/raw/master/install.sh && bash install.sh
# ----------------------------------------------------------------------

# abort on errors
set -e

port="$1"

# 安装docker
if ! [ -x "$(command -v docker)" ]; then
  echo '检测到 Docker 尚未安装，正在尝试安装 Docker ...'

  if [ -x "$(command -v yum)" ]; then
    sudo yum install -y python3-pip yum-utils device-mapper-persistent-data lvm2
    sudo yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
    yum list docker-ce --showduplicates | sort -r
    sudo yum install docker-ce
  else
    sudo apt-get update
    sudo dpkg --configure -a
    sudo apt-get install python3-pip apt-transport-https ca-certificates curl software-properties-common
    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
    sudo apt-get update
    sudo apt-get install docker-ce
  fi

  # 启动docker和开机自启动
  sudo systemctl start docker
  sudo systemctl enable docker
fi


 # 安装docker-compose
if ! [ -x "$(command -v docker-compose)" ]; then
  echo '检测到 Docker-Compose 尚未安装，正在尝试安装 Docker-Compose ...'
  if ! [ -x "$(command -v pip3)" ]; then
      curl -L https://gitee.com/zhongjie150/compose/releases/download/1.25.1/docker-compose-`uname -s`-`uname -m` -o /usr/local/bin/docker-compose
      chmod +x /usr/local/bin/docker-compose
  else
      pip3 install --upgrade pip
      pip3 install docker-compose
  fi
fi


 # 开始安装Fizz
if [ -x "$(command -v docker)" -a -x "$(command -v docker-compose)" ]; then
  docker version
  docker-compose -version

  # 安装Fizz
  if [ ! -f "docker-compose.yml" ];then
    wget https://gitee.com/fizzgate/fizz-gateway-node/raw/master/docker-compose.yml
  fi

  if [ ! -f "/etc/docker/daemon.json" ];then
    sudo mkdir -p /etc/docker
    echo -E '{"registry-mirrors": ["https://kn77wnbv.mirror.aliyuncs.com"]}' > /etc/docker/daemon.json
    sudo systemctl daemon-reload
    sudo systemctl restart docker
  fi

  docker-compose up -d

else

  if ! [ -x "$(command -v docker)" ]; then
    echo 'Docker 安装失败，请检测您当前的环境（或网络）是否正常。'
  fi


  if ! [ -x "$(command -v docker-compose)" ]; then
    echo 'Docker-Compose 安装失败，请检测您当前的环境（或网络）是否正常。'
  fi

fi

rm -f install.sh