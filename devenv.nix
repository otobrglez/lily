{ pkgs, lib, config, inputs, ... }:

{
  name = "lily";
  env.LILY_ENVIRONMENT = "development";

  languages.java.jdk.package = pkgs.jdk21_headless;
  languages.scala = {
    enable = true;
    sbt.enable = true;
  };

  packages = [ 
  	pkgs.git
    pkgs.kubectl
    pkgs.k9s
    pkgs.kubie
    pkgs.kubectx
    pkgs.kubernetes-helm
	pkgs.jq
	pkgs.yq-go
	pkgs.redis
  ];

  enterShell = ''
    echo "~~~ lily in $LILY_ENVIRONMENT ~~~"
    alias k='microk8s kubectl
  '';
  
  enterTest = ''
  	sbt test
  '';
}
