// Compiled by ClojureScript 1.9.908 {}
goog.provide('shop2.add_items');
goog.require('cljs.core');
goog.require('reagent.core');
goog.require('re_frame.core');
goog.require('clojure.string');
shop2.add_items.items = new cljs.core.PersistentVector(null, 5, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(0),new cljs.core.Keyword(null,"entryname","entryname",1434429085),"milk",new cljs.core.Keyword(null,"tags","tags",1771418977),"liquid"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(1),new cljs.core.Keyword(null,"entryname","entryname",1434429085),"cream",new cljs.core.Keyword(null,"tags","tags",1771418977),"liquid"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(2),new cljs.core.Keyword(null,"entryname","entryname",1434429085),"toast",new cljs.core.Keyword(null,"tags","tags",1771418977),"bakery"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(3),new cljs.core.Keyword(null,"entryname","entryname",1434429085),"pork",new cljs.core.Keyword(null,"tags","tags",1771418977),"meat"], null),new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"id","id",-1388402092),(4),new cljs.core.Keyword(null,"entryname","entryname",1434429085),"cheese",new cljs.core.Keyword(null,"tags","tags",1771418977),"dairy"], null)], null);
re_frame.core.reg_event_db.call(null,new cljs.core.Keyword(null,"initialize","initialize",609952913),(function (_,___$1){
return new cljs.core.PersistentArrayMap(null, 3, [new cljs.core.Keyword(null,"items","items",1031954938),shop2.add_items.items,new cljs.core.Keyword(null,"visible","visible",-1024216805),shop2.add_items.items,new cljs.core.Keyword(null,"search-str","search-str",-821246171),""], null);
}));
shop2.add_items.mk_add_item = (function shop2$add_items$mk_add_item(a_list,item){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"tr","tr",-1424774646),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"td.item-txt-td","td.item-txt-td",1140765574),new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"a.item-txt-a","a.item-txt-a",222086757),new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"href","href",-793805698),["/user/add-to-list/",cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"_id","_id",-789960287).cljs$core$IFn$_invoke$arity$1(a_list)),"/",cljs.core.str.cljs$core$IFn$_invoke$arity$1(new cljs.core.Keyword(null,"_id","_id",-789960287).cljs$core$IFn$_invoke$arity$1(item))].join('')], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"div.item-txt","div.item-txt",-1738604289),new cljs.core.Keyword(null,"entryname","entryname",1434429085).cljs$core$IFn$_invoke$arity$1(item)], null)], null)], null),new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Keyword(null,"td.item-tags-td","td.item-tags-td",1273934741),hf.label(new cljs.core.PersistentArrayMap(null, 1, [new cljs.core.Keyword(null,"class","class",-2030961996),"item-tags"], null),new cljs.core.Keyword(null,"x","x",2099068185),(function (){var G__20504 = item;
var G__20504__$1 = (((G__20504 == null))?null:new cljs.core.Keyword(null,"tags","tags",1771418977).cljs$core$IFn$_invoke$arity$1(G__20504));
if((G__20504__$1 == null)){
return null;
} else {
return common.frmt_tags(G__20504__$1);
}
})())], null)], null);
});
