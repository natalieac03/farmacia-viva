import type { Database, Lote, Saldo } from "@/types/estoque";
export const daysTo=(iso:string)=>Math.ceil((new Date(iso).getTime()-Date.now())/86400000);
export const vencido=(l:Lote)=>daysTo(l.validade)<0;
export const saldoLote=(s:Saldo[],id:string)=>s.filter(x=>x.loteId===id).reduce((a,x)=>a+x.quantidade,0);
export const itemNome=(db:Database,id:string)=>db.items.find(i=>i.id===id)?.nome??"Item desconhecido";
export const localNome=(db:Database,id?:string)=>db.locais.find(l=>l.id===id)?.nome??"—";
export const qualityTone=(q:string)=>q==="Aprovado"||q==="Não se aplica"?"good":q==="Pendente"||q==="Em quarentena"?"warn":q==="Bloqueado"||q==="Reprovado"?"danger":"neutral";
export const brDate=(v:string)=>new Intl.DateTimeFormat("pt-BR").format(new Date(v.length===10?v+"T12:00:00":v));
export const brDateTime=(v:string)=>new Intl.DateTimeFormat("pt-BR",{dateStyle:"short",timeStyle:"short"}).format(new Date(v));
