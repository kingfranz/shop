// Compiled by ClojureScript 1.9.908 {}
goog.provide('re_frame.fx');
goog.require('cljs.core');
goog.require('re_frame.router');
goog.require('re_frame.db');
goog.require('re_frame.interceptor');
goog.require('re_frame.interop');
goog.require('re_frame.events');
goog.require('re_frame.registrar');
goog.require('re_frame.loggers');
re_frame.fx.kind = new cljs.core.Keyword(null,"fx","fx",-1237829572);
if(cljs.core.truth_(re_frame.registrar.kinds.call(null,re_frame.fx.kind))){
} else {
throw (new Error("Assert failed: (re-frame.registrar/kinds kind)"));
}
/**
 * Register the given effect `handler` for the given `id`.
 * 
 *   `id` is keyword, often namespaced.
 *   `handler` is a side-effecting function which takes a single argument and whose return
 *   value is ignored.
 * 
 *   Example Use
 *   -----------
 * 
 *   First, registration ... associate `:effect2` with a handler.
 * 
 *   (reg-fx
 *   :effect2
 *   (fn [value]
 *      ... do something side-effect-y))
 * 
 *   Then, later, if an event handler were to return this effects map ...
 * 
 *   {...
 * :effect2  [1 2]}
 * 
 * ... then the `handler` `fn` we registered previously, using `reg-fx`, will be
 * called with an argument of `[1 2]`.
 */
re_frame.fx.reg_fx = (function re_frame$fx$reg_fx(id,handler){
return re_frame.registrar.register_handler.call(null,re_frame.fx.kind,id,handler);
});
/**
 * An interceptor whose `:after` actions the contents of `:effects`. As a result,
 *   this interceptor is Domino 3.
 * 
 *   This interceptor is silently added (by reg-event-db etc) to the front of
 *   interceptor chains for all events.
 * 
 *   For each key in `:effects` (a map), it calls the registered `effects handler`
 *   (see `reg-fx` for registration of effect handlers).
 * 
 *   So, if `:effects` was:
 *    {:dispatch  [:hello 42]
 *     :db        {...}
 *     :undo      "set flag"}
 * 
 *   it will call the registered effect handlers for each of the map's keys:
 *   `:dispatch`, `:undo` and `:db`. When calling each handler, provides the map
 *   value for that key - so in the example above the effect handler for :dispatch
 *   will be given one arg `[:hello 42]`.
 * 
 *   You cannot rely on the ordering in which effects are executed.
 */
re_frame.fx.do_fx = re_frame.interceptor.__GT_interceptor.call(null,new cljs.core.Keyword(null,"id","id",-1388402092),new cljs.core.Keyword(null,"do-fx","do-fx",1194163050),new cljs.core.Keyword(null,"after","after",594996914),(function re_frame$fx$do_fx_after(context){
var seq__20473 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"effects","effects",-282369292).cljs$core$IFn$_invoke$arity$1(context));
var chunk__20474 = null;
var count__20475 = (0);
var i__20476 = (0);
while(true){
if((i__20476 < count__20475)){
var vec__20477 = cljs.core._nth.call(null,chunk__20474,i__20476);
var effect_key = cljs.core.nth.call(null,vec__20477,(0),null);
var effect_value = cljs.core.nth.call(null,vec__20477,(1),null);
var temp__4655__auto___20483 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__4655__auto___20483)){
var effect_fn_20484 = temp__4655__auto___20483;
effect_fn_20484.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: no handler registered for effect: \"",effect_key,"\". Ignoring.");
}

var G__20485 = seq__20473;
var G__20486 = chunk__20474;
var G__20487 = count__20475;
var G__20488 = (i__20476 + (1));
seq__20473 = G__20485;
chunk__20474 = G__20486;
count__20475 = G__20487;
i__20476 = G__20488;
continue;
} else {
var temp__4657__auto__ = cljs.core.seq.call(null,seq__20473);
if(temp__4657__auto__){
var seq__20473__$1 = temp__4657__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__20473__$1)){
var c__8484__auto__ = cljs.core.chunk_first.call(null,seq__20473__$1);
var G__20489 = cljs.core.chunk_rest.call(null,seq__20473__$1);
var G__20490 = c__8484__auto__;
var G__20491 = cljs.core.count.call(null,c__8484__auto__);
var G__20492 = (0);
seq__20473 = G__20489;
chunk__20474 = G__20490;
count__20475 = G__20491;
i__20476 = G__20492;
continue;
} else {
var vec__20480 = cljs.core.first.call(null,seq__20473__$1);
var effect_key = cljs.core.nth.call(null,vec__20480,(0),null);
var effect_value = cljs.core.nth.call(null,vec__20480,(1),null);
var temp__4655__auto___20493 = re_frame.registrar.get_handler.call(null,re_frame.fx.kind,effect_key,false);
if(cljs.core.truth_(temp__4655__auto___20493)){
var effect_fn_20494 = temp__4655__auto___20493;
effect_fn_20494.call(null,effect_value);
} else {
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: no handler registered for effect: \"",effect_key,"\". Ignoring.");
}

var G__20495 = cljs.core.next.call(null,seq__20473__$1);
var G__20496 = null;
var G__20497 = (0);
var G__20498 = (0);
seq__20473 = G__20495;
chunk__20474 = G__20496;
count__20475 = G__20497;
i__20476 = G__20498;
continue;
}
} else {
return null;
}
}
break;
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch-later","dispatch-later",291951390),(function (value){
var seq__20499 = cljs.core.seq.call(null,value);
var chunk__20500 = null;
var count__20501 = (0);
var i__20502 = (0);
while(true){
if((i__20502 < count__20501)){
var map__20503 = cljs.core._nth.call(null,chunk__20500,i__20502);
var map__20503__$1 = ((((!((map__20503 == null)))?((((map__20503.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__20503.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.call(null,cljs.core.hash_map,map__20503):map__20503);
var effect = map__20503__$1;
var ms = cljs.core.get.call(null,map__20503__$1,new cljs.core.Keyword(null,"ms","ms",-1152709733));
var dispatch = cljs.core.get.call(null,map__20503__$1,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009));
if((cljs.core.empty_QMARK_.call(null,dispatch)) || (!(typeof ms === 'number'))){
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch-later value:",effect);
} else {
re_frame.interop.set_timeout_BANG_.call(null,((function (seq__20499,chunk__20500,count__20501,i__20502,map__20503,map__20503__$1,effect,ms,dispatch){
return (function (){
return re_frame.router.dispatch.call(null,dispatch);
});})(seq__20499,chunk__20500,count__20501,i__20502,map__20503,map__20503__$1,effect,ms,dispatch))
,ms);
}

var G__20507 = seq__20499;
var G__20508 = chunk__20500;
var G__20509 = count__20501;
var G__20510 = (i__20502 + (1));
seq__20499 = G__20507;
chunk__20500 = G__20508;
count__20501 = G__20509;
i__20502 = G__20510;
continue;
} else {
var temp__4657__auto__ = cljs.core.seq.call(null,seq__20499);
if(temp__4657__auto__){
var seq__20499__$1 = temp__4657__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__20499__$1)){
var c__8484__auto__ = cljs.core.chunk_first.call(null,seq__20499__$1);
var G__20511 = cljs.core.chunk_rest.call(null,seq__20499__$1);
var G__20512 = c__8484__auto__;
var G__20513 = cljs.core.count.call(null,c__8484__auto__);
var G__20514 = (0);
seq__20499 = G__20511;
chunk__20500 = G__20512;
count__20501 = G__20513;
i__20502 = G__20514;
continue;
} else {
var map__20505 = cljs.core.first.call(null,seq__20499__$1);
var map__20505__$1 = ((((!((map__20505 == null)))?((((map__20505.cljs$lang$protocol_mask$partition0$ & (64))) || ((cljs.core.PROTOCOL_SENTINEL === map__20505.cljs$core$ISeq$)))?true:false):false))?cljs.core.apply.call(null,cljs.core.hash_map,map__20505):map__20505);
var effect = map__20505__$1;
var ms = cljs.core.get.call(null,map__20505__$1,new cljs.core.Keyword(null,"ms","ms",-1152709733));
var dispatch = cljs.core.get.call(null,map__20505__$1,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009));
if((cljs.core.empty_QMARK_.call(null,dispatch)) || (!(typeof ms === 'number'))){
re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch-later value:",effect);
} else {
re_frame.interop.set_timeout_BANG_.call(null,((function (seq__20499,chunk__20500,count__20501,i__20502,map__20505,map__20505__$1,effect,ms,dispatch,seq__20499__$1,temp__4657__auto__){
return (function (){
return re_frame.router.dispatch.call(null,dispatch);
});})(seq__20499,chunk__20500,count__20501,i__20502,map__20505,map__20505__$1,effect,ms,dispatch,seq__20499__$1,temp__4657__auto__))
,ms);
}

var G__20515 = cljs.core.next.call(null,seq__20499__$1);
var G__20516 = null;
var G__20517 = (0);
var G__20518 = (0);
seq__20499 = G__20515;
chunk__20500 = G__20516;
count__20501 = G__20517;
i__20502 = G__20518;
continue;
}
} else {
return null;
}
}
break;
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch","dispatch",1319337009),(function (value){
if(!(cljs.core.vector_QMARK_.call(null,value))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch value. Expected a vector, but got:",value);
} else {
return re_frame.router.dispatch.call(null,value);
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"dispatch-n","dispatch-n",-504469236),(function (value){
if(!(cljs.core.sequential_QMARK_.call(null,value))){
return re_frame.loggers.console.call(null,new cljs.core.Keyword(null,"error","error",-978969032),"re-frame: ignoring bad :dispatch-n value. Expected a collection, got got:",value);
} else {
var seq__20519 = cljs.core.seq.call(null,cljs.core.remove.call(null,cljs.core.nil_QMARK_,value));
var chunk__20520 = null;
var count__20521 = (0);
var i__20522 = (0);
while(true){
if((i__20522 < count__20521)){
var event = cljs.core._nth.call(null,chunk__20520,i__20522);
re_frame.router.dispatch.call(null,event);

var G__20523 = seq__20519;
var G__20524 = chunk__20520;
var G__20525 = count__20521;
var G__20526 = (i__20522 + (1));
seq__20519 = G__20523;
chunk__20520 = G__20524;
count__20521 = G__20525;
i__20522 = G__20526;
continue;
} else {
var temp__4657__auto__ = cljs.core.seq.call(null,seq__20519);
if(temp__4657__auto__){
var seq__20519__$1 = temp__4657__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__20519__$1)){
var c__8484__auto__ = cljs.core.chunk_first.call(null,seq__20519__$1);
var G__20527 = cljs.core.chunk_rest.call(null,seq__20519__$1);
var G__20528 = c__8484__auto__;
var G__20529 = cljs.core.count.call(null,c__8484__auto__);
var G__20530 = (0);
seq__20519 = G__20527;
chunk__20520 = G__20528;
count__20521 = G__20529;
i__20522 = G__20530;
continue;
} else {
var event = cljs.core.first.call(null,seq__20519__$1);
re_frame.router.dispatch.call(null,event);

var G__20531 = cljs.core.next.call(null,seq__20519__$1);
var G__20532 = null;
var G__20533 = (0);
var G__20534 = (0);
seq__20519 = G__20531;
chunk__20520 = G__20532;
count__20521 = G__20533;
i__20522 = G__20534;
continue;
}
} else {
return null;
}
}
break;
}
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"deregister-event-handler","deregister-event-handler",-1096518994),(function (value){
var clear_event = cljs.core.partial.call(null,re_frame.registrar.clear_handlers,re_frame.events.kind);
if(cljs.core.sequential_QMARK_.call(null,value)){
var seq__20535 = cljs.core.seq.call(null,value);
var chunk__20536 = null;
var count__20537 = (0);
var i__20538 = (0);
while(true){
if((i__20538 < count__20537)){
var event = cljs.core._nth.call(null,chunk__20536,i__20538);
clear_event.call(null,event);

var G__20539 = seq__20535;
var G__20540 = chunk__20536;
var G__20541 = count__20537;
var G__20542 = (i__20538 + (1));
seq__20535 = G__20539;
chunk__20536 = G__20540;
count__20537 = G__20541;
i__20538 = G__20542;
continue;
} else {
var temp__4657__auto__ = cljs.core.seq.call(null,seq__20535);
if(temp__4657__auto__){
var seq__20535__$1 = temp__4657__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__20535__$1)){
var c__8484__auto__ = cljs.core.chunk_first.call(null,seq__20535__$1);
var G__20543 = cljs.core.chunk_rest.call(null,seq__20535__$1);
var G__20544 = c__8484__auto__;
var G__20545 = cljs.core.count.call(null,c__8484__auto__);
var G__20546 = (0);
seq__20535 = G__20543;
chunk__20536 = G__20544;
count__20537 = G__20545;
i__20538 = G__20546;
continue;
} else {
var event = cljs.core.first.call(null,seq__20535__$1);
clear_event.call(null,event);

var G__20547 = cljs.core.next.call(null,seq__20535__$1);
var G__20548 = null;
var G__20549 = (0);
var G__20550 = (0);
seq__20535 = G__20547;
chunk__20536 = G__20548;
count__20537 = G__20549;
i__20538 = G__20550;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return clear_event.call(null,value);
}
}));
re_frame.fx.reg_fx.call(null,new cljs.core.Keyword(null,"db","db",993250759),(function (value){
if(!((cljs.core.deref.call(null,re_frame.db.app_db) === value))){
return cljs.core.reset_BANG_.call(null,re_frame.db.app_db,value);
} else {
return null;
}
}));

//# sourceMappingURL=fx.js.map
