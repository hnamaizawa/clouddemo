/**
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates.
 * The Universal Permissive License (UPL), Version 1.0
 */
'use strict';

requirejs.config({
    baseUrl: 'js',

    // ロードする（可能性のある）JavaScriptライブラリの構成情報
    paths: {
        'knockout': 'libs/knockout/knockout-3.4.0',
        'jquery': 'libs/jquery/jquery-3.1.0.min',
        'jqueryui-amd': 'libs/jquery/jqueryui-amd-1.12.0.min',
        'promise': 'libs/es6-promise/es6-promise.min',
        'hammerjs': 'libs/hammer/hammer-2.0.8.min',
        'ojdnd': 'libs/dnd-polyfill/dnd-polyfill-1.0.0.min',
        'ojs': 'libs/oj/v2.1.0/min',
        'ojL10n': 'libs/oj/v2.1.0/ojL10n',
        'ojtranslations': 'libs/oj/v2.1.0/resources',
        'text': 'libs/require/text',
        'signals': 'libs/js-signals/signals.min'
    },
    // AMD (Asynchronous Module Definition; ライブラリのモジュール化や非同期ロードのためのお約束) に
    // 非対応のライブラリをモジュール化するための構成
    shim: {
        'jquery': {
            exports: ['jQuery', '$']
        }
    }
});
require([
        // このモジュールが依存しているモジュールたち
        "ojs/ojcore",
        "knockout",
        "jquery",
        "ojs/ojknockout",
        "ojs/ojmodel",
        "ojs/ojknockout-model",
        "ojs/ojarraytabledatasource",
        "ojs/ojpagingtabledatasource",
        "ojs/ojtable",
        "ojs/ojpagingcontrol",
        "ojs/ojchart",
        "ojs/ojbutton",
        'ojs/ojdialog',
        'ojs/ojtabs',
        'ojs/ojconveyorbelt',
        'ojs/ojmenu'
    ],
    function(oj, ko, $) {

        var contextRoot = "";

        // Employee リソースのレコードを表すオブジェクトの定義
        var EmployeeModel = oj.Model.extend({
            idAttribute: "employeeId",
            parse: function(response) {
                // JSON オブジェクトから ViewModel オブジェクトで使用する形式に変換する
                return {
                    employeeId: response["employeeId"],
                    firstName: response["firstName"],
                    lastName: response["lastName"],
                    jobId: response["job"]["jobId"],
                    jobTitle: response["job"]["jobTitle"],
                    salary: response["salary"],
                    department: response["department"]["departmentName"]
                };
            }
        });

        // Employee リソースのコレクション表すオブジェクトの定義
        var EmployeesCollection = oj.Collection.extend({
            url: location.protocol + "//" + location.host + contextRoot + "/api/hr/employees",
            model: new EmployeeModel()
        });

        // index.html 内の id="mainContent" の状態を保持する ViewModel
        function MainViewModel() {
            var self = this;

            // Knockout.jsによって監視されているので双方向データバインドが有効なプロパティ
            self.titleLabel = ko.observable("Java & Cloud Update Seminar");
            self.employees = ko.observableArray();
            self.chartGroups = ko.observableArray(["給与"]);

            self.username = ko.observable();
            self.password = ko.observable();


            // 表形式（ojTable コンポーネント）で表示されるデータのコレクション
            // self.employees に変更があるとコールバック関数が呼ばれる
            self.tableDataSource = ko.computed(function() {
                return new oj.PagingTableDataSource(new oj.ArrayTableDataSource(self.employees()));
            });

            // チャート（ojChart コンポーネント）で表示するためのを抽出
            // self.employees に変更があるとコールバック関数が呼ばれる
            // チャートのデータは次のようなオブジェクトの配列
            // { name: <シリーズデータの名前>, items: [ <グループ#1 の値>, <グループ#2の値>, ... ] }
            self.chartSeries = ko.computed(function() {
                var seriesValues = [];
                if (self.employees().length !== 0) {
                    var values = {};
                    $.each(self.employees(), function(index, emp) {
                        var jobId = emp.jobId();
                        var jobTitle = emp.jobTitle();
                        var salary = emp.salary();
                        if (values[jobId]) {
                            values[jobId].items[0] += salary;
                        } else {
                            values[jobId] = {
                                name: jobTitle,
                                items: [salary]
                            };
                        }
                    });
                    $.each(values, function(key, value) {
                        seriesValues.push(value);
                    });
                }
                return seriesValues;
            });

            // 更新ボタン押下のアクション
            self.handleOpen = $("#updateButton").click(function() {
                // 認証トークンはローカルストレージに保存しておくルールにする
                if (!sessionStorage.authToken) { // 認証されていない
                    $("#fetch_stat").text("ログインして下さい");
                } else {
                    fetchData();
                }
            });


            // 認証ダイアログ ボタン押下のアクション
            self.buttonClick = function(data, event) {
                $("#modalDialog1").ojDialog("close");
                //console.log(event);
                auth();
                return true;
            };

            // ログイン or ログアウトのリンク
            $("#logoutLink").click(function() {
                // 認証トークンはローカルストレージに保存しておくルールにする
                if (!sessionStorage.authToken) { // 認証されていない
                  if("SCIM" == sessionStorage.authType){
                      $("#modalDialog1").ojDialog("open");
                  }else if ("OAuth" == sessionStorage.authType) {
                      oauth();
                  }else{
                      alert("システム管理者に連絡して下さい: NO_AUTH_TYPE");
                  }
                } else {
                  if("SCIM" == sessionStorage.authType){
                      logout();
                  }else if ("OAuth" == sessionStorage.authType) {
                      oauth_logout();
                  }else{
                    alert("システム管理者に連絡して下さい: NO_AUTH_TYPE");
                  }
                }
            });

            // OAuth認証処理
            function oauth() {
              $("#logoutLink").text("");
              $("#updateButton").prop("disabled", true);
              document.body.style.cursor = 'wait';
              $("#fetch_stat").text("認証中");
              $.ajax({
                  url: location.protocol + "//" + location.host + contextRoot + "/api/oauth/start",
              }).done(function(data, textStatus, jqXHR) {
                  //alert(data.call_uri)
                  var child = window.open(data.call_uri, 'auth_window', 'width=800, height=512, menubar=no, toolbar=no, scrollbars=no, resizable=no');
                  var state = data.state;
                  $.ajax({
                      url: location.protocol + "//" + location.host + contextRoot + "/api/oauth/result?state=" + state,
                  }).done(function(data, textStatus, jqXHR) {
                      $("#updateButton").prop("disabled", false);
                      document.body.style.cursor = 'auto';
                      $("#fetch_stat").text("");
                      sessionStorage.authToken = jqXHR.getResponseHeader("X-AUTH-TOKEN");
                      sessionStorage.username = data.name;
                      updateLoginStatus();
                      child.close();
                  }).fail(function(jqXHR, textStatus, errorThrown) {
                      $("#updateButton").prop("disabled", false);
                      document.body.style.cursor = 'auto';
                      $("#fetch_stat").text("ログインできません (" + jqXHR.status + ")");
                      updateLoginStatus();
                      //alert(jqXHR.responseText);
                      child.close();
                  });
              }).fail(function(jqXHR, textStatus, errorThrown) {
                  $("#updateButton").prop("disabled", false);
                  document.body.style.cursor = 'auto';
                  $("#fetch_stat").text("ログインできません (" + jqXHR.status + ")");
                  updateLoginStatus();
                  //alert(jqXHR.responseText);
              });
            }

            function oauth_logout() {
                $("#updateButton").prop("disabled", true);
                document.body.style.cursor = 'wait';
                $("#fetch_stat").text("ログアウト中");
                var authToken = sessionStorage.authToken;
                sessionStorage.removeItem('authToken');
                $.ajax({
                    url: location.protocol + "//" + location.host + contextRoot + "/api/oauth/logout_start",
                    headers: {
                        'X-AUTH-TOKEN': authToken
                    }
                }).done(function(data, textStatus, jqXHR) {
                    //alert(data.call_uri)
                    var child = window.open(data.call_uri, 'auth_window', 'width=800, height=256, menubar=no, toolbar=no, scrollbars=no, resizable=no');
                    var state = data.state;
                    $.ajax({
                        url: location.protocol + "//" + location.host + contextRoot + "/api/oauth/logout_result?state=" + state,
                    }).done(function(data, textStatus, jqXHR) {
                        $("#updateButton").prop("disabled", false);
                        document.body.style.cursor = 'auto';
                        $("#fetch_stat").text("");
                        self.employees([]); // 表示データはクリア ここでやるのは見栄えの問題だけ
                        updateLoginStatus();
                        child.close();
                    }).fail(function(jqXHR, textStatus, errorThrown) {
                        $("#updateButton").prop("disabled", false);
                        document.body.style.cursor = 'auto';
                        $("#fetch_stat").text("ログアウト中にエラー発生 (" + jqXHR.status + ")");
                        self.employees([]); // 表示データはクリア ここでやるのは見栄えの問題だけ
                        updateLoginStatus();
                        //alert(jqXHR.responseText);
                        child.close();
                    });
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    $("#updateButton").prop("disabled", false);
                    // OAuthログアウト処理
                    document.body.style.cursor = 'auto';
                    $("#fetch_stat").text("");
                    self.employees([]); // 表示データはクリア ここでやるのは見栄えの問題だけ
                    updateLoginStatus();
                    oj.Logger.error("Error: " + jqXHR.responseText);
                });
            }

            // 認証処理
            function auth() {
                $("#logoutLink").text("");
                $("#updateButton").prop("disabled", true);
                document.body.style.cursor = 'wait';
                $("#fetch_stat").text("認証中");
                var password = self.password();
                self.password(undefined);
                $.ajax({
                    url: location.protocol + "//" + location.host + contextRoot + "/api/auth/in",
                    headers: {
                        'X-AUTH-USER': self.username(),
                        'X-AUTH-PASS': password
                    }
                }).done(function(data, textStatus, jqXHR) {
                    $("#updateButton").prop("disabled", false);
                    document.body.style.cursor = 'auto';
                    $("#fetch_stat").text("");
                    sessionStorage.authToken = jqXHR.getResponseHeader("X-AUTH-TOKEN");
                    sessionStorage.username = data.username;
                    updateLoginStatus();
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    $("#updateButton").prop("disabled", false);
                    document.body.style.cursor = 'auto';
                    $("#fetch_stat").text("ログインできません (" + jqXHR.status + ")");
                    updateLoginStatus();
                    //alert(jqXHR.responseText);
                });
            }

            //ログアウト処理
            function logout() {
                $("#updateButton").prop("disabled", true);
                document.body.style.cursor = 'wait';
                $("#fetch_stat").text("ログアウト中");
                var authToken = sessionStorage.authToken;
                sessionStorage.removeItem('authToken');
                $.ajax({
                    url: location.protocol + "//" + location.host + contextRoot + "/api/auth/out",
                    headers: {
                        'X-AUTH-TOKEN': authToken
                    }
                }).done(function(data, textStatus, jqXHR) {
                    $("#updateButton").prop("disabled", false);
                    document.body.style.cursor = 'auto';
                    $("#fetch_stat").text("");
                    self.employees([]); // 表示データはクリア ここでやるのは見栄えの問題だけ
                    updateLoginStatus();
                }).fail(function(jqXHR, textStatus, errorThrown) {
                    $("#updateButton").prop("disabled", false);
                    document.body.style.cursor = 'auto';
                    $("#fetch_stat").text("");
                    self.employees([]); // 表示データはクリア ここでやるのは見栄えの問題だけ
                    updateLoginStatus();
                    oj.Logger.error("Error: " + jqXHR.responseText);
                });
            }

            // Collectionのインスタンスを生成
            function fetchData() {
                $("#updateButton").prop("disabled", true);
                $("#logoutLink").prop("disabled", true);
                document.body.style.cursor = 'wait';
                $("#fetch_stat").text("データ取得中");
                self.empCollection = new EmployeesCollection();
                self.empCollection.fetch({
                    headers: {
                        'X-AUTH-TOKEN': sessionStorage.authToken
                    },
                    success: function(collection, response, options) {
                        // サービス呼び出しが成功した時の実行されるコールバック関数
                        $("#updateButton").prop("disabled", false);
                        $("#logoutLink").prop("disabled", true);
                        document.body.style.cursor = 'auto';
                        self.employees(oj.KnockoutUtils.map(collection));
                        $("#server_name").text("接続先: " + options.xhr.getResponseHeader("X-SERVER-NAME"));
                        $("#fetch_stat").text("");
                    },
                    error: function(jqXHR, textStatus, errorThrown) {
                        $("#updateButton").prop("disabled", false);
                        $("#logoutLink").prop("disabled", true);
                        document.body.style.cursor = 'auto';
                        $("#server_name").text("接続先: " + jqXHR.getResponseHeader("X-SERVER-NAME"));
                        //oj.Logger.error("Error: " + textStatus);
                        if (jqXHR.status == 401) {
                            //sessionStorage.authToken = null; // これはif(!authToken)が効かない！
                            sessionStorage.removeItem('authToken');
                            $("#fetch_stat").text("セッション・タイムアウトしました");
                            updateLoginStatus();
                        } else { //console.log(jqXHR);
                            $("#fetch_stat").text("エラー (" + jqXHR.status + ")");
                            alert(jqXHR.responseText);
                        }
                    }
                });
            }
        };

        function updateLoginStatus(){
          if (sessionStorage.authToken) {
              $("#logoutLink").text("ログアウト");
              $("#welcome").text("ようこそ " + sessionStorage.username + " さん! ");
          } else {
              $("#logoutLink").text("ログイン");
              $("#welcome").text("");
          }
          document.activeElement.blur(); // フォーカスを外す
        }

        function getConfig(){
          $.ajax({
              url: location.protocol + "//" + location.host + contextRoot + "/api/config",
          }).done(function(data, textStatus, jqXHR) {
              sessionStorage.authType = data.auth_type;
          }).fail(function(jqXHR, textStatus, errorThrown) {
              alert("初期化に失敗しました.");
              oj.Logger.error("Error: " + jqXHR.responseText);
          });

        }

        $(function() {
            function init() {
                ko.applyBindings(new MainViewModel(), document.getElementById('mainContent'));
                getConfig();
                updateLoginStatus();
            }

            // If running in a hybrid (e.g. Cordova) environment, we need to wait for the deviceready
            // event before executing any code that might interact with Cordova APIs or plugins.
            if ($(document.body).hasClass('oj-hybrid')) {
                document.addEventListener('deviceready', init);
            } else {
                init();
            }
        });

    });
