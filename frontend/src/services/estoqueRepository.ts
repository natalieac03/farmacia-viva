import { createDemoData } from "@/mocks/demoData";
import { databaseSchema, type Database, type Item, type Lote, type NovaMovimentacao, type Saldo } from "@/types/estoque";

export const STORAGE_KEY = "cremic-estoque-demo-v1";
export class StorageCorrompidoError extends Error {}
type EntradaLote = Omit<Lote,"id"|"itemId"|"criadoEm"|"atualizadoEm"|"situacao">;

function migrarDadosLocais(value: unknown): unknown {
  if (typeof value !== "object" || value === null || !("schemaVersion" in value)) {
    throw new StorageCorrompidoError("A versão dos dados locais não foi identificada.");
  }
  const version=(value as {schemaVersion:unknown}).schemaVersion;
  if (version === 1) return value;
  throw new StorageCorrompidoError(`A versão local ${String(version)} não é compatível com este protótipo.`);
}

export interface EstoqueRepository {
  carregar(): Database;
  salvar(db: Database): void;
  restaurar(): Database;
  movimentar(input: NovaMovimentacao): Database;
  registrarEntrada(item: Omit<Item,"id"|"ativo">|null, lote: EntradaLote, itemId: string|undefined, quantidade: number, localId: string, responsavel: string): Database;
  finalizarLote(loteId: string, motivo: string, responsavel: string): Database;
}

export function calcularSaldos(db: Database): Saldo[] {
  const map = new Map<string, Saldo>();
  const add = (itemId:string,loteId:string,localId:string|undefined,q:number) => {
    if (!localId) return;
    const key=`${itemId}|${loteId}|${localId}`;
    const saldo=map.get(key)??{itemId,loteId,localId,quantidade:0};
    saldo.quantidade+=q;
    map.set(key,saldo);
  };
  db.movimentos.forEach(m => {
    if (m.tipo === "Transferência") {
      add(m.itemId,m.loteId,m.origemId,-m.quantidade);
      add(m.itemId,m.loteId,m.destinoId,m.quantidade);
    } else {
      const negative=["Saída","Ajuste negativo"].includes(m.tipo);
      add(m.itemId,m.loteId,m.destinoId??m.origemId,negative?-m.quantidade:m.quantidade);
    }
  });
  return [...map.values()].filter(s=>Math.abs(s.quantidade)>0.000001);
}

class LocalEstoqueRepository implements EstoqueRepository {
  carregar(): Database {
    const raw=localStorage.getItem(STORAGE_KEY);
    if (!raw) return this.restaurar();
    try { return databaseSchema.parse(migrarDadosLocais(JSON.parse(raw))); }
    catch { throw new StorageCorrompidoError("Os dados locais não puderam ser validados."); }
  }
  salvar(db: Database) { localStorage.setItem(STORAGE_KEY,JSON.stringify(databaseSchema.parse(db))); }
  restaurar() { const db=createDemoData(); this.salvar(db); return db; }
  movimentar(input: NovaMovimentacao) {
    const db=this.carregar();
    const lote=db.lotes.find(l=>l.id===input.loteId);
    if (!lote) throw new Error("Lote não encontrado.");
    if (lote.situacao!=="Ativo") throw new Error("Somente lotes ativos podem ser movimentados.");
    const saida=["Saída","Transferência","Ajuste negativo"].includes(input.tipo);
    if (saida && ["Bloqueado","Reprovado","Em quarentena"].includes(lote.qualidade)) throw new Error("O lote não está liberado para saída.");
    if (saida && input.origemId) {
      const atual=calcularSaldos(db).find(s=>s.loteId===input.loteId&&s.localId===input.origemId)?.quantidade??0;
      if (atual<input.quantidade) throw new Error(`Saldo insuficiente. Disponível: ${atual} ${input.unidade}.`);
    }
    db.movimentos.unshift({...input,id:crypto.randomUUID(),dataHora:new Date().toISOString(),situacao:"Confirmada"});
    this.salvar(db); return db;
  }
  registrarEntrada(item:Omit<Item,"id"|"ativo">|null,lote:EntradaLote,itemId:string|undefined,quantidade:number,localId:string,responsavel:string) {
    if (!(quantidade>0) || !localId || !responsavel.trim() || !lote.codigo || !lote.validade) throw new Error("Preencha lote, validade, quantidade, local e responsável.");
    const db=this.carregar();
    const selected=item?{...item,id:crypto.randomUUID(),ativo:true}:db.items.find(i=>i.id===itemId);
    if (!selected) throw new Error("Selecione ou cadastre um item.");
    if (item) db.items.push(selected);
    const now=new Date().toISOString();
    const novoLote={...lote,id:crypto.randomUUID(),itemId:selected.id,situacao:"Ativo" as const,criadoEm:now,atualizadoEm:now};
    db.lotes.push(novoLote);
    db.movimentos.unshift({id:crypto.randomUUID(),tipo:"Entrada",itemId:selected.id,loteId:novoLote.id,quantidade,unidade:selected.unidade,destinoId:localId,motivo:"Registro de entrada",observacao:lote.observacoes,responsavel,dataHora:now,situacao:"Confirmada"});
    this.salvar(db); return db;
  }
  finalizarLote(loteId:string,motivo:string,responsavel:string) {
    if (!motivo.trim()||!responsavel.trim()) throw new Error("Informe motivo e responsável.");
    const db=this.carregar(); const lote=db.lotes.find(l=>l.id===loteId);
    if (!lote) throw new Error("Lote não encontrado.");
    Object.assign(lote,{situacao:"Finalizado",finalizadoEm:new Date().toISOString(),motivoFinalizacao:motivo.trim(),responsavel,atualizadoEm:new Date().toISOString()});
    this.salvar(db); return db;
  }
}
export const estoqueRepository: EstoqueRepository = new LocalEstoqueRepository();
