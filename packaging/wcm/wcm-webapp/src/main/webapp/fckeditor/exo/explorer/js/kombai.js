	/*
	* Kombai - create database in browser.
	* Create by uoon (vnjs.net)
	*/
	var Kombai = {
		archive: {
			data: {},
			method: {},
			variable: {}
		},
		K: {
			add: {},
			get: {},
			set: {},
			check: {},
			remove: {},
			create: {},
			insert: {},
			select: {},
			request: {}
		}
	};
	
	//begin function at Do Son - Hai Phong;
	var K = function(R) {
		var E = document.getElementById(R) || R;
		if (E.String || K.check.isString(E)) {
			E = E.String || E;
			if (!E.isEncode) {
				E = new String(E);
			}
			E.trim = function() {
				var ADN = this;
				ADN = K.remove.space(ADN);
				return K({String: ADN});
			};
			E.encode = function() {
				var ADN = this;
				var A = [];
				var L = ADN.length;
				for (var o = 0 ; o < L; ++ o) {
					A.push(ADN.charCodeAt(o));
				}
				ADN = new String(A.join("."));
				ADN.isEncode = true;
				return K({String: ADN});
			};
			E.decode = function() {
				var ADN = this;
				if (ADN.isEncode) {
					var A = ADN.split(".");
					var L = A.length;
					ADN = new String();
					for (var o = 0; o < L; ++ o) {
						ADN += String.fromCharCode(A[o]);
					}
				} 
				return K({String: ADN});
			};
			E.urlEncode = function() {
				var ADN = this;
				return K({String: encodeURIComponent(ADN)});
			};
			E.urlDecode = function() {
				var ADN = this;
				return K({String: decodeURIComponent(ADN)});
			};
		} else if (E.Element || K.check.isElement(E)) {
			E = E.Element || E;
			if (K.check.isString(R)) {
				R = K({String: R});
				E.trim = function() {
					return R.trim();
				};
				E.encode = function() {
					return R.encode();
				};
				E.decode = function() {
					return R.decode();
				};
				E.urlEncode = function() {
					return R.urlEncode();
				};
				E.urlDecode = function() {
					return R.urlDecode();
				};
			}
			E.X = function() {
				var ADN = this;
				return ADN.style.left;
			};
			E.Y = function() {
				var ADN = this;
				return ADN.style.top;
			};
			E.add = function(r) {
				var ADN = this;
				r.element = ADN;
				K.add(r);
				return K({Element: ADN});
			};
			E.hide = function() {
				var ADN = this;
				ADN.style.display = "none";
				return K({Element: ADN});
			};
			E.show = function() {
				var ADN = this;
				ADN.style.display = "block";
				ADN.style.visibility = "visible";
				return K({Element: ADN});
			};
			E.opacity = function(r) {
				var ADN = this;
				if (K.check.isNumber(r)) {
					if (K.check.isIE()) {
						ADN.style.filter = "alpha(opacity = r)".replace("r", r);
					} else {
						ADN.style.opacity = r/100;
					}
				}
				return K({Element: ADN});
			};
			E.initDragDrop = function(r) {
				if (r === undefined) {
					var r = {};
				}
				var ADN = this;
				ADN.store = {
					x: 0,
					y: 0,
					X: 0,
					Y: 0
				};
				ADN.event = {  
					mouseDown: function() {},
					mouseMove: function() {},
					mouseUp: function() {}
				};
				ADN.run = {
					drag: r.onDrag || function() {},
					move: r.onMove || function() {},
					drop: r.onDrop || function() {}			
				};
				ADN.drop = function() {
					ADN.store = null;
					ADN.event = null;
					ADN.run = null;
					ADN.drop = function() {};
					ADN.onmousedown = ignore;
				};
				ADN.penetrate = function(enviroment) {
					if (!K.check.isObject(enviroment)) {
						return;
					}
					var Xmin = K.get.X(enviroment);
					var Xmax = Xmin + enviroment.offsetWidth;
					var Ymin = K.get.Y(enviroment);
					var Ymax = Ymin + enviroment.offsetHeight;
					var X = ADN.store.X;
					var Y = ADN.store.Y;
					if (Xmin < X && X < Xmax && Ymin < Y && Y < Ymax) {
						return true;
					} else {
						return false;
					}
				};
				ADN.event.mouseDown = function(event) {
					var event = (event || window.event);
					if (!(event.button == 2 || event.which == 3)) {
						if (isNaN(parseInt(ADN.style.left))) {
							ADN.style.left = "0px";
						}
						if (isNaN(parseInt(ADN.style.top))) {
							ADN.style.top = "0px";
						}
						if (!ADN.style.position || ADN.style.position == "static") {
							ADN.style.position = "relative";
						}
						
						ADN.store.x = event.clientX;
						ADN.store.y = event.clientY;
						ADN.event.mouseMove = function(event) {
							var event = (event || window.event);
							ADN.run.move.call(ADN);
							ADN.style.left = parseInt(ADN.style.left) + (event.clientX - ADN.store.x) + "px";
							ADN.style.top = parseInt(ADN.style.top) + (event.clientY - ADN.store.y) + "px";
							ADN.store.x = event.clientX;
							ADN.store.y = event.clientY;
							ADN.store.X = K.get.X(event);
							ADN.store.Y = K.get.Y(event);
							return false;
						};
						ADN.event.mouseUp = function() {
							document.onmousemove = null;
							ADN.event.mouseMove = null;
							ADN.event.mouseUp = null;
							ADN.onmouseup = null;
							ADN.run.drop.call(E);
						};
						ADN.run.drag.call(ADN);
						ADN.onmouseup = ADN.event.mouseUp;
						document.onmousemove = ADN.event.mouseMove;
						document.onmouseup = function(event) {
							var S = K.get.eventSource(event);
							if (S != ADN) {
								document.onmousemove = null;
							}
						};
						return false;
					}
				};
				ADN.onmousedown = ADN.event.mouseDown;
				function ignore(event) {
					var event = event || window.event;
					event.cancelBubble = true;
				}
				return K({Element: ADN});
			};
			E.select = function(r) {
				var ADN = this;
				r.from = ADN;
				return K.select(r);
			};
			E.insert = function(r) {
				var ADN = this;
				r.refer = ADN;
				K.insert(r);
				return K({Element: ADN});
			};
			E.slow = function(r) {
				var ADN = this;
				var setting = {
					method: r.begin || function() {},
					until: r.end || function() {return true;},
					param: [ADN],
					delay: 100,
					release: r.release || function() {}
				}
				K.set.timeout(setting);
			};
		} else if (E.Function || K.check.isFunction(E)) {
			E = E.Function || E;
			E.callBack = function(r) {
				var ADN = this;
				var setting = {
					param: r.param !== undefined ? r.param : [],
					recall: {
						method: (r.recall.method !== undefined) ? r.recall.method : function() {},
						param: (r.recall.param !== undefined) ? r.recall.param : []
					}
				};
				ADN.apply(ADN, setting.param);
				if (K.check.isString(setting.recall.method)) {
					eval(setting.recall.method);
				} else {
					setting.recall.method.apply(setting.recall.method, setting.recall.param);
				}
			};
			E.repeat = function(r) {
				var ADN = this;
				r.method = ADN;
				K.set.timeout(r);
			};
		}
		try {
			return E;
		} finally {
			E = null;
		}
	};
	
	Kombai.K = K;
	for (Kombai.archive.variable.property in Kombai.K) {
		K[Kombai.archive.variable.property] = Kombai.K[Kombai.archive.variable.property];
	}
	delete Kombai.archive.variable.property;
	
	K.add = function(r) {
		var setting = {
			element: null,
			//add className
			className: null,
			//add event
			event: null,
			listener: null,
			//add attribute
			attribute: null,
			value: null,
			//add method
			method: null,
			source: null,
			//add childNode
			childNode: null
		};
		
		for (var o in setting) {
			if (r[o] !== undefined) {
				setting[o] = r[o];
			}	
		}
		
		Add();
		
		function Add() {
			with (setting) {
				if (!element) {
					return;
				}	
				if (className != null) {
					addClassName(element, className);
				}	
				if (attribute != null && value != null) {
					addAttribute(element, attribute, value);
				}
				if (childNode != null) {
					addChildNode(element, childNode);
				}
				if (method != null && source != null) {
					addMethod(element, method, source);
				}
				if (event != null && listener != null) {
					addEvent(element, event, listener);
				}
			}
		}
		
		function addClassName(E, C) {
			if (E.className != "") {
				E.className += " " + C;
			} else {
				E.className = C;
			}
		}
		
		function addEvent(E, T, L) {
			if (E.addEventlistenerer) {
	            E.addEventlistenerer(T, L, false);
	        } else if (E.attachEvent) {
	            E.attachEvent("on" + T, L);
	        } else {
				if (E["on" + T] instanceof Function) {
					var oL = E["on" + T];
					E["on" + T] = function(event) {
						oL(event);
						L(event);
					};
				} else {
					E["on" + T] = L;
				}
			}
		}
		
		function addAttribute(E, A, V) {
			E.setAttribute(A, V);
		}
		
		function addMethod(E, M, S) {
			E[M] = S;
		}
		
		function addChildNode(E, N) {
			E.appendChild(N);
		}
	};
	
	K.get = {
		generalId: function() {
			return (new Date().getTime() + Math.random().toString().substring(2));
		},
		browserName: function() {
			return navigator.appName;
		},
		domain: function() {
			return document.domain;
		},
		height: function(r) {
			var element = r || document.getElementById(r);
			if (element.height !== undefined) {
				return element.height;
			} else {
				return element.offsetHeight;
			}
		},
		width: function(r) {
			var element = r || document.getElementById(r);
			if (element.width !== undefined) {
				return element.width;
			} else {
				return element.offsetWidth;
			}
		},
		element: function(r) {
			return document.getElementById(r);
		},
		screenHeight: function() {
			return screen.availHeight;
		},
		screenWidth: function() {
			return screen.availWidth;
		},
		windowHeight: function() {
			if (window.innerHeight !== undefined) {
				return window.innerHeight;
			} else if (document.documentElement) {
				return document.documentElement.clientHeight;
			} else {
				return document.body.clientHeight;
			}
		},
		windowWidth: function() {
			if (window.innerWidth !== undefined) {
				return window.innerWidth;
			}
			else if (document.documentElement) {
				return document.documentElement.clientWidth;
			} else {
				return document.body.clientWidth;
			}
		},
		pageHeight: function() {
			return document.body.scrollHeight;
		},
		pageWidth: function() {
			return document.body.scrollWidth;
		},
		currentStyle: function(E, P) {
			if (E.constructor && E.constructor == String) {
				E = document.getElementById(E);
			}
			if (E.currentStyle) {  
			  var A = P.split("-");
			  var N = A.length;
			  for (var o = 1; o < N; ++ o) {
				 A[o] = A[o].replace(/\w/, A[o].charAt(0).toUpperCase());
			  }
			  P = A.join("");
			  return E.currentStyle[P];
			} else if (document.defaultView && document.defaultView.getComputedStyle) {
			  return document.defaultView.getComputedStyle(E, null).getPropertyValue(P);
			} else {
				return null;
			}
		},
		currentTime: function() {
			return new Date().getTime();
		},
		eventSource: function(event) {
			return (event && event.target) ? event.target : window.event.srcElement;
		},
		topPage: function() {
			if (window.pageYOffset !== undefined) {
				return window.pageYOffset;
			} else if (document.documentElement !== undefined) {
				return document.documentElement.scrollTop;
			} else {
				return document.body.scrollTop;
			}
		},
		bottomPage: function() {
			if (window.pageYOffset !== undefined) {
				return (window.pageYOffset + window.innerHeight);
			} else if (document.documentElement !== undefined) {
				return (document.documentElement.scrollTop + document.documentElement.clientHeight);
			} else {
				return (document.body.scrollTop + document.body.clientHeight);
			}
		},
		leftPage: function() {
			if (window.pageXOffset !== undefined) {
				return window.pageXOffset;
			} else if (document.documentElement !== undefined) {
				return document.documentElement.scrollLeft;
			} else {
				return document.body.scrollLeft;
			}
		},
		rightPage: function() {
			if (window.pageXOffset !== undefined) {
				return (window.pageXOffset + window.innerWidth);
			} else if (document.documentElement !== undefined) {
				return (document.documentElement.scrollLeft + document.documentElement.clientWidth);
			} else {
				return (document.body.scrollLeft + document.body.clientWidth);
			}
		},
		X: function(r) {
			if (r.constructor && r.constructor == String) {
				r = document.getElementById(r);
			}
			var E = r || window.event;
			if (E.nodeName !== undefined) {
				var X = 0;
				while (E) {
					X += E.offsetLeft;
					E = E.offsetParent;
				}
				return X;
			} else if (E.clientX !== undefined) {
				return (E.clientX + document.body.scrollLeft + document.documentElement.scrollLeft);
			}
		},
		Y: function(r) {
			if (r.constructor && r.constructor == String) {
				r = document.getElementById(r);
			}
			var E = r || window.event;
			if (E.nodeName !== undefined) {
				var Y = 0;
				while (E) {
					Y += E.offsetTop;
					E = E.offsetParent;
				}
				return Y;
			} else if (E.clientY !== undefined) {
				return (E.clientY + document.body.scrollTop + document.documentElement.scrollTop);
			}
		}
	};

	K.set = {
		timeout: function(r) {
			var setting = {
				method: function() {},
				delay: 1000,
				param: [],
				until: function() {
					return false;
				},
				amount: 4294967295,
				release: function() {}
			};
			var n = 0;
			for (var o in setting) {
				if (r[o]) {
					setting[o] = r[o];
				}
			}
			function repeat() {
				if (setting && !setting.until() && (n < setting.amount)) {
					setting.method.apply(setting.method, setting.param);
					setTimeout(repeat, setting.delay);
					++ n;
				} else {
					setting.release();
					setting = null;
				}
			};
			setTimeout(repeat, setting.delay);
		},
		event: function(r) {
			var setting = {
				element: null,
				type: null,
				listener: function() {}
			};
			for (var o in setting) {
				if (r[o] !== undefined) {
					setting[o] = r[o];
				} else {
					return;
				}
			}
			setting.element["on" + setting.type] = setting.listener;
		}
	};
	
	K.check = {
		isSafari: function() {
			return new RegExp("Safari").test(navigator.userAgent);
		},
		isOpera: function() {
			return new RegExp("Opera").test(navigator.userAgent);
		},
		isFF: function() {
			return new RegExp("Firefox").test(navigator.userAgent);
		},
		isIE: function() {
			return new RegExp("MSIE").test(navigator.userAgent);
		},
		isIE6: function() {
			return new RegExp("MSIE 6").test(navigator.userAgent);
		},
		isIE7: function() {
			return new RegExp("MSIE 7").test(navigator.userAgent);
		},
		isIE8: function() {
			return new RegExp("MSIE 8").test(navigator.userAgent);
		},
		isFunction: function(r) {
			return (r != null && r.constructor == Function);
		},
		isString: function(r) {
			return (r != null && r.constructor == String);
		},
		isNumber: function(r) {
			return (r != null && r.constructor == Number);
		},
		isArray: function(r) {
			return (r != null && r.constructor == Array);
		},
		isRegExp: function(r) {
			return (r != null && r.constructor == RegExp);
		},
		isObject: function(r) {
			return (r != null && r instanceof Object);
		},
		isElement: function(r) {
			return (r != null && r.nodeType);
		}
	};
	
	K.remove = {
		element: function(r) {
			if (r && r.constructor == String) {
				r = document.getElementById(r);
			}
			if (r.parentNode) {
				r.parentNode.removeChild(r);
			}
		}, 
		timeout: function(r) {
			clearInterval(r);
			clearTimeout(r);
			r = null;
			delete r;
		},
		space: function(r) {
			if (r && r.constructor == String) {
				return r.replace(/^\s+|\s+$/g, "");
			} else {
				return new String();
			}
		},
		tag: function(r) {
			if (r && r.constructor == String) {
				return r.replace(/<\/?[^>]+>/gi, "");
			} else {
				return new String();
			}
		},
		event: function(r) {
			var setting = {
				element: null,
				type: null,
				listener: null
			};
			for (var o in r) {
				if (r[o] !== undefined) {
					setting[o] = r[o];
				} else {
					return;
				}
			}
			with (setting) {
				if (element.removeEventListener) { 
					element.removeEventListener(type, listener, false);
				} else if (element.detachEvent) {
					element.detachEvent("on" + type, listener);
				} else {  
					element["on" + type] = null;
				}
			}
		}
	};
	
	K.create = {
		database: function(r) {
			return new function() {
				if (!(r && r instanceof Object)) {
					alert("Don't create database");
					return;
				}
				//begin config database;
				var generalId = new Date().getTime() + Math.random().toString().substring(2);
				var reference = "7c4bc080af27ded752b36160dcb1c716";
				var ADN = this;
				var setting = {
					fromCollection: null,
					rowIndex: 0,
					stepJump: 1,
					startRange: 1,
					fieldName: [],
					primaryKey: null,
					autoIncrement: null,
					ignoreError: false
				};
				for (var o in setting) {
					if (r[o]) {
						setting[o] = r[o];
					}
				}
				if (!setting.fromCollection) {
					ADN.Row = [];
					//check config database
					if (!setting.fieldName || setting.fieldName.constructor != Array || !setting.fieldName.length) {
						alert("fieldName is not null");
						return;
					} else if (setting.primaryKey) {
						var checkPrimaryKey = false;
						var length = setting.fieldName.length;
						for (var o = 0; o < length; ++ o) {
							if (setting.primaryKey == setting.fieldName[o]) {
								checkPrimaryKey = true;
								break;
							}
						}
						if (!checkPrimaryKey) {
							if (!setting.ignoreError) {
								alert("please preview primaryKey");
							}
							return false;
						}
					}
					//seting for autooIncrement field;
					if (setting.autoIncrement != null && setting.autoIncrement.constructor == Object) {
						setting.startRange = setting.autoIncrement[1].start;
						setting.stepJump = setting.autoIncrement[1].step;
						setting.autoIncrement =  setting.autoIncrement[0];
					}
					//check autoIncrement key;
					if (setting.autoIncrement) {
						var checkAutoIncrement = false;
						for (var o in setting.fieldName) {
							if (setting.autoIncrement == setting.fieldName[o]) {
								checkAutoIncrement = true;
								break;
							}
						}
						if (checkAutoIncrement) {
							setting.fieldName.splice(o, 1);
							setting.fieldName.push(setting.autoIncrement);
						} else {
							if (!setting.ignoreError) {
								alert("please preview autoIncrement key");
							}
							return false;
						}
					}
				} else {
					ADN.Row = setting.fromCollection;
					with (setting) {
						if (fromCollection.length) {
							rowIndex = fromCollection.length - 1;
						} else {
							if (!ignoreError) {
								alert("fromCollection is not empty");
							}
							return false;
						}
						stepJump = 1;
						startRange = 1;
						fieldName = null;
						primaryKey = null;
						autoIncrement = null;
					}
				}
				// end config database;
				
				ADN.Drop = function() {
					ADN.Row = [];
					with (setting) {
						rowIndex = 0;
						startRange = 0;
						stepJump = 1;
					}
				}
				
				ADN.Count = function() {
					return ADN.Row.length;
				}
				
				ADN.Check = function(content) {
					if (!content.Condition) {
						return false;
					} else {
						content.Condition = parseCondition(content.Condition);
					}
					for (var o in ADN.Row) {
						with (ADN.Row[o]) {
							if (eval(content.Condition)) {
								return true;
							}
						}
					}
					return false;
				}//end method Check;	
				
				ADN.Insert = function(data) {
					if (!data && data.constructor && data.constructor != Object) {
						if (!setting.ignoreError) {
							alert("Can't insert value into table");
						}
						return;
					}
					// create mask object;
					var nSize = setting.fieldName.length;
					if (nSize) {
						var newObject = {};
						for (var o = 0; o < nSize; ++ o) {
							if (!setting.autoIncrement || (setting.fieldName[o] != setting.autoIncrement)) {
								var cellValue = data[o];
								if (cellValue == null) {
									cellValue = data[setting.fieldName[o]];
								}
								newObject[setting.fieldName[o]] = (cellValue != null) ? cellValue : null;
							} else if (setting.fieldName[o] == setting.autoIncrement) {
								var checkAutoValue = data[o];
								if (checkAutoValue == null) {
									checkAutoValue = data[setting.fieldName[o]];
								}
								if ((checkAutoValue != null) && (checkAutoValue != setting.startRange)) {
									if (!setting.ignoreError) {
										alert("cannot insert value = "+ checkAutoValue + " in to autoIncrement field " + setting.autoIncrement);
									}
									return;
								} else {
									newObject[setting.fieldName[o]] = setting.startRange;
									setting.startRange += setting.stepJump;
								}
							} 
						}
						//check unique value;
						nSize = ADN.Count();
						for (var o = 0; o < nSize; ++ o) {
							if (setting.primaryKey == null || setting.primaryKey == "") {
								break;
							} else if (ADN.Row[o][setting.primaryKey] == newObject[setting.primaryKey]) {
								if (!setting.ignoreError) {
									alert("value of primary key '" + setting.primaryKey + "' : " + newObject[setting.primaryKey] + " has exits in row : " + o);
								}
								return;
							}
						}//end check;
					} else {
						var newObject = data;
					}
					// create new record;
					ADN.Row[setting.rowIndex] = newObject;
					setting.rowIndex += 1;
				}//end method Insert;
				
				ADN.Select = function(condition) {
					var noneCondition = (condition == undefined || condition.constructor != Object);
					var config = {
						fieldName : setting.fieldName,
						Where : null,
						orderBy : null,
						Limit: ADN.Row.length
					};
					config.Begin = 0;
					config.aMask = [];
					var aData = [];
					if (noneCondition) {
						aData = ADN.Row;
					} else {
						for (var o in config) { 
							if (condition[o]) {
								config[o] = condition[o];
							}
						}
						for (var o in ADN.Row) {
							config.aMask[o] = ADN.Row[o]; 
						}
						config.Where = parseCondition(config.Where);
						parseLimitClause();
						parseOrderByClause();
						aData = selectData();
					}
					
					aData.View = function(extendView) {
						if (extendView && extendView instanceof Function) {
							extendView.call(aData);
						} else {
							alert("Don't view this data");
						}
					}
					
					//return result;
					return aData;
					
					/**************/
					//private function
					function parseLimitClause() {
						if (config.Limit.constructor == Object) {
							config.Begin = config.Limit[0];
							config.Limit = config.Limit[1];
						}
					}//end function Limit;
					
					function parseOrderByClause() {
						if (config.orderBy == undefined || config.orderBy == null) {
							return;
						}
						var aOrder = [];
						if (config.orderBy.indexOf(",") > 0) {
							aOrder = config.orderBy.split(",");
						} else 	{
							aOrder[0] = config.orderBy;
						}
						/*
						input : orderBy : "a+b+c ASC, d DESC";
						*/
						var length = aOrder.length;
						for (var o = 0; o < length; ++ o) {
							aHead = aOrder.slice(0, o);
							aFoot = aOrder.slice(o, aOrder.length);
							if (aOrder[o].indexOf("+") > 0) {
								aBody = aOrder[o].split("+");
								oEnd = aBody[aBody.length - 1];
								var regExp = new RegExp("\\b(\\w+)\\b(.*)\\b","i");
								if (regExp.test(oEnd)) {
									aReg = regExp.exec(oEnd);
									aBody[aBody.length - 1] = aReg[1];
									if (aReg[2] == "") {
										aReg[2] = "asc";
									}
									for (var i = 0; i < aBody.length; i ++ ) {
										aBody[i] = aBody[i] + " " + aReg[2];
									}
								}
								//remove the last of aFoot;
								aFoot.splice(0, 1);
								//build the array aOrder;
								aOrder = aHead.concat(aBody).concat(aFoot);
							}
						}
						/*
						ouput : a ASC, b ASC, c ASC, d DESC;
						*/
						for (var o = 0; o < length; ++ o) {
							var aClause = [];
							var	sClause = "";
							for (var i = 0; i <= o; ++ i) {		
								isOrder = new RegExp("\\b(\\w+)\\b(.*)\\b","i");
								if (isOrder.test(aOrder[i])) {
									aReg = isOrder.exec(aOrder[i]);
									if (aReg[2].toLowerCase().indexOf("desc") > -1) {
										aClause.push("(oCurrent." + aReg[1] + " <= oNext." + aReg[1] + ")");
									} else if (aReg[2].toLowerCase().indexOf("asc") > -1 || aReg[2] == "") {
										aClause.push("(oCurrent." + aReg[1] + " >= oNext." + aReg[1] + ")");
									}
								}
							}
							if (aClause.length > 0) {
								sClause = aClause.join("&&");
							}
							var oDepository = {};
							var oCurrent = {};
							var oNext = {};
							var nSize = config.aMask.length;
							for (var current = 0; current < nSize; ++ current) {
								for (var next = eval(current + 1); next < nSize; ++ next) {
									oCurrent = config.aMask[current];
									oNext = config.aMask[next];
									if (eval(sClause)) {
										oDepository = config.aMask[current];
										config.aMask[current] = config.aMask[next];
										config.aMask[next] = oDepository;
									}
								}
							}
						}//end for order;
					}//end function orderBy;
					
					function selectData() {
						var nIndex = 0;
						var aResult = [];
						var nSize = config.aMask.length;
						for (var element = config.Begin; element < nSize && config.Begin < config.Limit; ++ element) {
							if (config.aMask[element]) {
								with (config.aMask[element]) {
									if (eval(config.Where)) {
										if (setting.fromCollection) {
											aResult[nIndex] = config.aMask[element];
										} else {
											var nWidth = config.fieldName.length;
											aResult[nIndex] = {};
											for (var column = 0; column < nWidth; ++ column) {
												aResult[nIndex][config.fieldName[column].toString()] = config.aMask[element][config.fieldName[column].toString()];
											}
										}
										++ config.Begin;
										++ nIndex;
									}
								}
							}
						}
						return aResult;
					}// end function selectData;
				}//end method Select;

				ADN.Delete = function(condition) {
					if (setting.fromCollection) {
						return;
					}
					var config = {Where: null};
					if (condition.Where) {
						config.Where = parseCondition(condition.Where);
					}
					for (var o in ADN.Row) {
						with (ADN.Row[o]) {
							if (eval(config.Where)) {
								ADN.Row.splice(o, 1); 
								setting.rowIndex -= 1;
							}
						}
					}
				}//end method Delete;
				
				ADN.Update = function(data) {
					var config = {
						rowSet : {},
						Where : false
					};
					for (var o in data) {
						if (o == "Where") {
							config.Where = data.Where;
							config.Where = parseCondition(config.Where);
						} else {
							config.rowSet[o] = data[o];
						}
					}
					//check value of primary key;
					var length = ADN.Count();
					for (var o = 0; o < length; ++ o) {
						for (var delta in config.rowSet) {
							if (delta == setting.primaryKey) {
								if (ADN.Row[o][delta] == config.rowSet[delta]) {
									if (!setting.ignoreError) {
										alert("value of primary key '" + delta + "' : " + config.rowSet[delta] + ", it has exits in row : " + o);
									}
									return;
								}
							}
						}
					}
					var checkCondition = false;
					for (var o = 0; o < length; ++ o) {
						// check condition;
						checkCondition = false;
						with (ADN.Row[o]) {
							if (eval(config.Where)) {
								checkCondition = true; 
							}
						}
						//update value to record;
						if (checkCondition) {
							for (var delta in config.rowSet) {
								if (delta == setting.autoIncrement)	{
									if (config.rowSet[delta] != ADN.Row[o][delta]) {
										if (!setting.ignoreError) {
											alert("cannot update value = "+ config.rowSet[delta] + " in to autoIncrement field : " + setting.autoIncrement);
										}
										return;
									}
								} else {
									ADN.Row[o][delta] = config.rowSet[delta];
								}
							}
						}
					}// end the loop;
					return false;
				}//end method Update;
				
				/*******************************/
				// private method.
				function parseCondition(condition) {
					var condition = Like(condition);
						condition = Between(condition);
					if (condition == "" || !condition) {
						return 1;
					} else {
						return condition;
					}
					
					/**************/
					//private function
					function Between(r) {
						var isBetween = new RegExp("^\\s*(.+)\\s+(between)\\s+(.*)\\s+(and)\\s+(.*)\\s*$","i");
						var notBetween = new RegExp("^\\s*(.+)\\s+(not between)\\s+(.*)\\s+(and)\\s+(.*)\\s*$","i");
						if (notBetween.test(r)) {
							var aBetween= notBetween.exec(r);
								aBetween[3] = eval(aBetween[3]);
								aBetween[5] = eval(aBetween[5]);
							if (aBetween[3].constructor && aBetween[3].constructor == String) {
								aBetween[3] = "'" + aBetween[3] + "'";
							}
							if (aBetween[5].constructor && aBetween[5].constructor == String) {
								aBetween[5] = "'" + aBetween[5] + "'";
							}
							if (aBetween[3] > aBetween[5]) {
								var oDepository = aBetween[3];
								aBetween[3] = aBetween[5];
								aBetween[5] = oDepository;
							}
							return "(" + aBetween[3] + " > " + aBetween[1] + ") || (" + aBetween[1] + " > " + aBetween[5] + ")";
						} else if (isBetween.test(r)) {
							var aBetween = isBetween.exec(r);
								aBetween[3] = eval(aBetween[3]);
								aBetween[5] = eval(aBetween[5]);
							if (aBetween[3].constructor && aBetween[3].constructor == String) {
								aBetween[3] = "'" + aBetween[3] + "'";
							}
							if (aBetween[3].constructor && aBetween[3].constructor == String) {
								aBetween[5] = "'" + aBetween[5] + "'";
							}
							if (aBetween[3] > aBetween[5]) {
								var oDepository = aBetween[3];
								aBetween[3] = aBetween[5];
								aBetween[5] = oDepository;
							}
							return "(" + aBetween[3] + " < " + aBetween[1] + ") && (" + aBetween[1] + " < " +aBetween[5] + ")";
						} else {
							return r;
						}
					}//end function Between;
					
					function Like(r) {
						var isLike = new RegExp("^\\s*(.+)\\s+(like)\\s*(')\\s*(.*)\\s*(')\\s*$", "i");
						var notLike = new RegExp("^\\s*(.+)\\s+(not like)\\s*(')\\s*(.*)\\s*(')\\s*$", "i");
						var reject = /^\s+|\s+$/ ;
						var like = false;
						if (notLike.test(r)) {
							var aLike = notLike.exec(r);
							var isFalse = " == ";
							var isTrue = " != ";
							like = true;
						} else if (isLike.test(r)) {
							var aLike = isLike.exec(r);
							var isFalse = " != ";
							var isTrue = " == ";
							like = true;
						}	
						if (like) {
							aLike[1] = aLike[1].toString();
							aLike[4] = aLike[4].toString();
							aLike[1] = aLike[1].replace(reject, "");
							aLike[4] = aLike[4].replace(reject, "");
							var regFull = /^%(.*)%$/;
							var regLeft = /^%(.*)/;
							var regRight = /(.*)%$/;
							if (regFull.test(aLike[4])) {
								aLike[4] = regFull.exec(aLike[4])[1];
								return (aLike[1] + ".toString().indexOf('" + aLike[4] + "')" + isFalse + "-1");
							} else if (regLeft.test(aLike[4])) {
								aLike[4] = regLeft.exec(aLike[4])[1];
								return (aLike[1] + ".toString().lastIndexOf('" + aLike[4] + "')" + isTrue + "(" + aLike[1] + ".length - " + aLike[4].length + ")");
							} else if (regRight.test(aLike[4])) {
								aLike[4] = regRight.exec(aLike[4])[1];
								return (aLike[1] + ".toString().indexOf('" + aLike[4] + "')" + isTrue + "0");
							} else {
								return (aLike[1] + isTrue + "'" + aLike[4] + "'");
							}
						} else {
							return r;
						}
					}//end function Like;
					/**************/
				}//end method  parseCondition;
				/*******************************/
			}// end class;
		},//end method  at Tam Diep - Ninh Binh;
			
		element: function(r) {
			var setting = {
				tagName: "div",
				innerHTML: "",
				className: ""
			};
			for (var o in setting) {
				if (r[o]) {
					setting[o] = r[o];
				}
			}
			var newNode = document.createElement(setting.tagName);
			if (setting.innerHTML) {
				newNode.innerHTML = setting.innerHTML;
			}
			if (setting.className) {
				newNode.className = setting.className;
			}
			return newNode;
		}
	};
	
	K.insert = function(r) {
		var setting = {
			element: document.createElement("div"),
			refer: document.body,
			where: "beforeEnd"
		};
		for (var o in setting) {
			if (r[o]) {
				setting[o] = r[o];
			}
		}
		with (setting) { 
			if ((/beforeBegin/i).test(where)) {
				refer.parentNode.insertBefore(element, refer);
			} else if ((/afterBegin/i).test(where)) {
				setting.refer.insertBefore(element, refer.firstChild);
			} else if ((/beforeEnd/i).test(where)) {
				refer.appendChild(element);
			} else if ((/afterEnd/i).test(where)) {
				refer.parentNode.insertBefore(element, refer.nextSibling);
			}
		}
	};
	
	K.select = function(r) {
		var setting = {
			from: document,
			where: null
		};
		for (var o in setting) {
			if (r[o]) {
				setting[o] = r[o];
			}
		}
		if (setting.from && setting.from.constructor == Array) {
			var DOM = K.create.database({fromCollection: setting.from})
		} else {
			var aElement = setting.from.getElementsByTagName("*");
			if (aElement && aElement.length) {
				var nSize = aElement.length;
				var aCollection = [];
				for (var o = 0; o < nSize; ++ o) {
					aCollection.push(aElement[o]);
				}
				var DOM = K.create.database({fromCollection: aCollection});
			}
		}
		return DOM.Select({Where: setting.where});
	};
	
	Kombai.archive.data = {
		oldRequest: K.create.database({
			fieldName : ["index", "setting", "compare", "responseText", "responseXML"],
			autoIncrement: "index",
			ignoreError: true
		})
	};
	
	K.request = function(r) {
		var oldRequest = Kombai.archive.data.oldRequest;
		if (!r || r.constructor != Object) {
			return;
		}
		return new function() {
			this.request = false;
			var oSelf = this;
			var oAjax = {};
			var	sADN = new String();
			var setting =  {
				address: null,
				method: "POST",
				data: null,
				async: true,
				retry: 0,
				delay: 1000,
				onRequest: null,
				onSuccess: null,
				onFailure: null,
				onAbort: null
			};
			var bCache = r.cache || false;
			for (var o in setting) {
				if (r[o]) {
					setting[o] = r[o];
				}
			}
			var nCount = r.reload ? setting.retry - r.reload : setting.retry;
			for (var o in setting) {
				sADN += o + ":" + encodeURIComponent(setting[o]) + "XOXOX"; 
			}
			var aResult = oldRequest.Select({Where: 'compare == "' + sADN + '"'});
			if (bCache && aResult[0]) {
				oAjax.responseText = aResult[0].responseText || null;
				oAjax.responseXML = aResult[0].responseXML || null;
				onSuccess(oAjax);
			} else {
				oAjax = createRequest();
				if (!oAjax) {
					return;
				}
				oSelf.request = true;
				if (isReady(oAjax)) {
					if (setting.method.toUpperCase() == "POST") {
						oAjax.open(setting.method, setting.address, setting.async);
						oAjax.setRequestHeader("Content-type", "application/x-www-form-urlencoded; charset=UTF-8");
						oAjax.setRequestHeader("Connection", "close");
						oAjax.send(setting.data);
					} else {
						oAjax.open(setting.method, setting.address + "?" + setting.data + "&R=" + new Date().getTime(), setting.async);
						oAjax.send(null);
					}
					onRequest(oAjax);
				} 
				oAjax.onreadystatechange = function() {
					if (!isComplete(oAjax) || !oSelf.request) {
						return;
					}
					onStateChange(oAjax);
				}
			}
			
			function createRequest() {		
				if (window.ActiveXObject) {
					return new ActiveXObject("Microsoft.XMLHTTP");
				} else {
					return new XMLHttpRequest() || null;
				}
			}
			
			function onStateChange(XHR) {
				oSelf.request = false;
				if (isSuccess(XHR)) {
					onSuccess(XHR);
					if (bCache) {
						oldRequest.Insert([setting, sADN, oAjax.responseText, oAjax.responseXML]);
					}
				} else {
					if (!nCount) {
						onFailure(XHR);
					} else {
						r.reload = r.reload ? r.reload + 1 : 1;
						oSelf.stop();
						if (oSelf.repeat) {
							clearTimeout(oSelf.repeat);
						}
						oSelf.repeat = setTimeout(
							function() {
								K.request(r);
							},
							setting.delay
						);
					}
				}
				delete oAjax.onreadystatechange;
			}
			
			function isReady(XHR) {
				return (XHR.readyState == 4 || XHR.readyState == 0);
			}
			
			function isRequest(XHR) {
				return XHR.readyState < 4;
			}
			
			function isComplete(XHR) {
				return XHR.readyState == 4;
			}
			
			function isSuccess(XHR) {
				try {
					return XHR.status == 200;
				} catch(e) {
					return false;
				}  
			}
			
			function onRequest(XHR) {
				if (setting.onRequest instanceof Function) {
					setting.onRequest.call(XHR);
				} else {
					eval(setting.onRequest);
				}
			}
			
			function onSuccess(XHR) {
				if (setting.onSuccess instanceof Function) {
					setting.onSuccess.call(XHR);
				} else {
					eval(setting.onSuccess);
				}
			}
			
			function onFailure(XHR) {
				if (setting.onFailure  instanceof Function) {
					setting.onFailure.call(XHR);
				} else {
					eval(setting.onFailure);
				}
			}
			
			function onAbort(XHR) {
				if (setting.onAbort instanceof Function) {
					setting.onAbort.call(XHR);
				} else {
					eval(setting.onAbort);
				}
			}
			
			this.stop = function() {
				delete oAjax.onreadystatechange;
				this.request = false; 
				onAbort(oAjax);
				oAjax.abort();
			}
		}
	};
		
	/****************************************/	

	K.discover = function(object) {
		remove();
		if (!object) return;
		var root = document.createElement("div");
			root.setAttribute("id" , "K.o.m.b.a.i");
			root.style.position = "absolute";
			root.style.top = "0px";
			root.style.width = "0px";
			root.style.height = "0px";
		var info = document.createElement("div");
			info.style.background = "black" ;
			info.style.top = "0px" ;
			info.style.color = "white" ;
			info.style.padding = "3px" ;
			info.style.width = "600px" ;
			info.style.zIndex = "9999" ;
			info.style.position = "relative" ;
		var closeButton = document.createElement("div");
			closeButton.style.padding = "3px 0px 6px 0px";
			closeButton.innerHTML = "<span style='cursor: pointer;' onclick='K.discover(window);'> {..} window </span>";
			closeButton.innerHTML += "<span style='cursor: pointer;' onclick='K.discover(window.document);'> / document </span>";
		var rightButton = document.createElement("div");
			rightButton.style.textAlign = "right";
			rightButton.style.margin = "-16px 0px 0px 200px";
		var trueClose  = document.createElement("div");
			trueClose.innerHTML = "<span style='color: red; font-weight: bold; cursor: pointer;'>[ X ]</span>" ;
		var blockContent = 	document.createElement("div");
			blockContent.style.background = "#848484" ;
			blockContent.style.border = "1px solid green" ;
			blockContent.style.height = "300px" ;
			blockContent.style.overflow = "auto" ;
			blockContent.style.padding = "10px" ;
			document.body.appendChild(root);
			root.appendChild(info);
			info.appendChild(closeButton);
			info.appendChild(blockContent);
			closeButton.appendChild(rightButton);
			rightButton.appendChild(trueClose);
			trueClose.onclick = remove;
		
		function inspect(o) {
			var node = document.createElement("div");
			if (/object/.test(typeof o)) {
				var v = {};
				for (var p in o) {
					try {v = o[p];}
					catch(e) {v = "Can't Access !!!";}
					if (/object/.test(typeof v)) {
						var div = document.createElement("div");						
						div.innerHTML = "<span style='margin-right: 2px;'>{..}</span>";
						div.innerHTML += "<span style='color: #9b1a00'>" + p + "</span> : " + v;
						node.appendChild(div);
					} else if (/string/.test(typeof v)) {
						var div = document.createElement("div");
						div.innerHTML = "<span style='margin-right: 15px;'>-</span>";
						div.innerHTML += "<span style='color: #9b1a00'>" + p + "</span> : " + v.replace(/</g, "&lt;");
						node.appendChild(div);
					} else {
						var div = document.createElement("div");
						div.innerHTML = "<span style='margin-right: 15px;'>-</span>";
						div.innerHTML += "<span style='color: #9b1a00'>" + p + "</span> : " + v;
						node.appendChild(div);
					}
				}
			} else if (/string/.test(typeof v)) {
				node.innerHTML = o.replace(/</g, "&lt;");
			} else {
				node.innerHTML = o;
			}
			return node;
		}
		
		function show(target, data) {
			target.appendChild(data);
		}
		
		function remove() {
			if (document.getElementById("K.o.m.b.a.i")) {
				var root = document.getElementById("K.o.m.b.a.i");
				root.parentNode.removeChild(root);
			}
		}
		show(blockContent, inspect(object));
	} ;
	
	Kombai.archive.method = {
		attachEvent: K.create.database({
			fieldName: ["code", "listener"],
			primaryKey: "code",
			ignoreError: true
		}),
		onResize: function(event) {
			var attachEvent = Kombai.archive.method.attachEvent;
			var aStore = attachEvent.Select({Where: "code like 'onResize%'"});
			var nLength = aStore.length;
			for (var o = 0; o < nLength; ++ o) {
				aStore[o].listener(event);
			}
		},
		onLoad: function(event) {
			var attachEvent = Kombai.archive.method.attachEvent; 
			var aStore = attachEvent.Select({Where: "code like 'onLoad%'"});
			var nLength = aStore.length;
			for (var o = 0; o < nLength; ++ o) {
				aStore[o].listener(event);
			}
		}
	};
	
	K.addEventOnLoad = function(r) {
		var attachEvent = Kombai.archive.method.attachEvent; 
		var source = K(r.toString()).encode().toString();
		attachEvent.Insert(["onLoad:" + source, r]);
	};
	
	K.addEventOnResize = function(r) {
		var attachEvent = Kombai.archive.method.attachEvent; 
		var source = K(r.toString()).encode.toString();
		attachEvent.Insert(["onResize:" + source, r]);
	};
	
	K.add({element: window, event: "load", listener: Kombai.archive.method.onLoad});
	K.add({
		element: window,
		event: "resize",
		listener: function() {
			if (Kombai.archive.variable.delay) {
				K.remove.timeout(Kombai.archive.variable.delay);
			}
			Kombai.archive.variable.delay = setTimeout(Kombai.archive.method.onResize, 1000);
		}
	});