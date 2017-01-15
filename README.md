# cloudseminar201612
Demo source of Java and Oracle Cloud Platform seminar @OAC on Dec 2nd, 2016

## **デモ本体 emplist-api & emplist-root**

### モジュール
* ROOT.war - static contents / Javascript
* api.war - RESTサービス

其々 mvn package して出来あがった2つのwarをデプロイします  
Tomcatの場合はデプロイする際にデフォルトのROOTを削除してROOT.warを配置して下さい（ルートコンテキストで動かす場合）  
pom-weblogic.xml は `mvn -f pom-weblogic.xml pre-integration-test` でWebLogicにデプロイするように仕込んであります

### maven pom
* pom.xml Tomcat用
* pom-weblogic.xml WebLogic用


### 環境変数
* DBAAS_DEFAULT_CONNECT_DESCRIPTOR (JDBCの接続ディスクリプタ)  
* STATE_SERVICE_URL (ステートサーバーの接続URL)  
* AUTH_TYPE (SCIM/OAuth/デフォルト SCIM)  
* AUTH_CLASS (AUTH_TYPE=SCIMの時に利用する認証クラス)  
* AUTH_SESSION_TIMEOUT (認証タイムアウト:分/デフォルト30分)  

以下は  
`AUTH_TYPE=OAuth` 若しくは
`AUTH_TYPE=SCIM
&& AUTH_CLASS=com.example.auth.IDCSAuthenticator`
の時に設定が必要  
* AUTH_IDCS_URL (IDCSの接続URL)  kote3564

* AUTH_IDCS_HOST (IDCSに接続する際にRequestのHostヘッダを上書きする値/オプション)  
* AUTH_IDCS_CLIENT_ID (IDCSアプリケーションのクライアントId)  
* AUTH_IDCS_CLIENT_SECRET (IDCSアプリケーションのクライアントSecret)  
* AUTH_IDCS_CALLBACK_SCHEME (IDCSのコールバックのプロトコルを強制する http/https)  

AUTH_IDCS_HOSTを設定した場合は、Java起動オプションに  
-Dsun.net.http.allowRestrictedHeaders=true  
を設定しないと有効になりません

## **ステートキャッシュ state-service**

デモアプリが認証ステータスを保持するためのとりあえずのキャッシュサーバーです  
ACCS向けの軽量JAX-RSサーバーの作り方としては標準的なものと思います  
そのままmvn packageするとgrizzly+JerseyベースのRESTサーバがACCS用ZIPとして作成されます  
ローカルでてっとり早く動かしたい場合は mvn exec:java で即起動します

### ACCSへのデプロイ

アプリケーションを最初に作成するときは  
`mvn -Daccs.deploy=create pre-integration-test`  

デプロイしたアプリケーションを更新するときは  
`mvn pre-integration-test`  

### Gradleによるbuild
`gradle zip` で build/distributions ディレクトリにACCS用のzipファイルを作成します  
`gradle execute` でサーバーを起動します  
ACCSへのデプロイ/更新/削除は、それぞれ  
`gradle -Daccs.create=true deployToACCS`  
`gradle deployToACCS`  
`gradle deleteFromACCS`  
を実行して下さい
