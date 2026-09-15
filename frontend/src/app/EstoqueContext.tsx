import { createContext, useContext, useMemo, useState, type ReactNode } from "react";
import { calcularSaldos, estoqueRepository, StorageCorrompidoError } from "@/services/estoqueRepository";
import type { Database, Item, Lote, NovaMovimentacao } from "@/types/estoque";

type EntradaArgs={item:Omit<Item,"id"|"ativo">|null;lote:Omit<Lote,"id"|"itemId"|"criadoEm"|"atualizadoEm"|"situacao">;itemId?:string;quantidade:number;localId:string;responsavel:string};
type ContextValue={db:Database|null;erro:string|null;saldos:ReturnType<typeof calcularSaldos>;movimentar:(v:NovaMovimentacao)=>void;registrarEntrada:(v:EntradaArgs)=>void;finalizar:(id:string,m:string,r:string)=>void;restaurar:()=>void};
const Context=createContext<ContextValue|null>(null);
export function EstoqueProvider({children}:{children:ReactNode}){
  const initial=()=>{try{return estoqueRepository.carregar()}catch(e){return null}};
  const [db,setDb]=useState<Database|null>(initial);
  const [erro,setErro]=useState<string|null>(()=>db?null:"Os dados locais estão corrompidos. Restaure a demonstração para continuar.");
  const value=useMemo<ContextValue>(()=>({db,erro,saldos:db?calcularSaldos(db):[],movimentar:v=>{setDb(estoqueRepository.movimentar(v));setErro(null)},registrarEntrada:v=>setDb(estoqueRepository.registrarEntrada(v.item,v.lote,v.itemId,v.quantidade,v.localId,v.responsavel)),finalizar:(id,m,r)=>setDb(estoqueRepository.finalizarLote(id,m,r)),restaurar:()=>{setDb(estoqueRepository.restaurar());setErro(null)}}),[db,erro]);
  return <Context.Provider value={value}>{children}</Context.Provider>;
}
export function useEstoque(){const c=useContext(Context);if(!c)throw new StorageCorrompidoError("Contexto de estoque indisponível");return c;}
