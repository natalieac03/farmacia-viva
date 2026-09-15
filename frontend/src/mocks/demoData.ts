import type { Database, Movimento } from "@/types/estoque";

const now = new Date();
const date = (days:number) => { const d=new Date(now); d.setDate(d.getDate()+days); return d.toISOString().slice(0,10); };
const movement = (id:string, itemId:string, loteId:string, quantidade:number, destinoId:string, days:number):Movimento => ({ id, tipo:"Entrada", itemId, loteId, quantidade, unidade:itemId==="i5"?"L":"kg", destinoId, motivo:"Recebimento demonstrativo", observacao:"Registro fictício para avaliação", responsavel:"Equipe PET-Saúde", dataHora:new Date(now.getTime()+days*86400000).toISOString(), situacao:"Confirmada" });

export function createDemoData():Database {
  const items = [
    {id:"i1",codigo:"FIT-GUA",nome:"Guaco",tecnica:"Fitoterapia" as const,apresentacao:"Planta seca",unidade:"kg",estoqueMinimo:4,ativo:true},
    {id:"i2",codigo:"FIT-ESP",nome:"Espinheira-santa",tecnica:"Fitoterapia" as const,apresentacao:"Planta processada",unidade:"kg",estoqueMinimo:3,ativo:true},
    {id:"i3",codigo:"HOM-GEL",nome:"Gelsemium",tecnica:"Homeopatia" as const,apresentacao:"Matriz homeopática",unidade:"mL",estoqueMinimo:500,ativo:true},
    {id:"i4",codigo:"FLO-DEM",nome:"Essência floral demonstrativa",tecnica:"Essências florais" as const,apresentacao:"Solução estoque",unidade:"mL",estoqueMinimo:300,ativo:true},
    {id:"i5",codigo:"GER-ALC",nome:"Álcool de cereais",tecnica:"Uso geral" as const,apresentacao:"Veículo",unidade:"L",estoqueMinimo:10,ativo:true},
  ];
  const base={origemTexto:"Dados demonstrativos",exigeCQ:false,observacoes:"Lote fictício, sem vínculo com pacientes.",situacao:"Ativo" as const,criadoEm:now.toISOString(),atualizadoEm:now.toISOString(),responsavel:"Equipe PET-Saúde"};
  const lotes = [
    {...base,id:"l1",codigo:"GUA-2601",itemId:"i1",tipo:"Planta seca" as const,origem:"Compra de terceiro" as const,fabricante:"Fornecedor demonstrativo",recebimento:date(-45),validade:date(240),qualidade:"Aprovado" as const,exigeCQ:true,lcq:"LCQ-DEM-014",laudoAgronomico:"AGR-DEM-009"},
    {...base,id:"l2",codigo:"ESP-2511",itemId:"i2",tipo:"Planta processada" as const,origem:"Produção interna CREMIC" as const,recebimento:date(-120),validade:date(18),qualidade:"Aprovado" as const,ordemProducao:"OP-REFERÊNCIA-021",laudoMicrobiologico:"MIC-DEM-033"},
    {...base,id:"l3",codigo:"GEL-2407",itemId:"i3",tipo:"Insumo" as const,origem:"Compra de terceiro" as const,recebimento:date(-500),validade:date(-20),qualidade:"Aprovado" as const},
    {...base,id:"l4",codigo:"FLO-2602",itemId:"i4",tipo:"Insumo" as const,origem:"Doação" as const,recebimento:date(-14),validade:date(300),qualidade:"Em quarentena" as const,exigeCQ:true,lcq:"Aguardando emissão"},
    {...base,id:"l5",codigo:"ALC-2601",itemId:"i5",tipo:"Insumo" as const,origem:"Compra de terceiro" as const,recebimento:date(-32),validade:date(500),qualidade:"Bloqueado" as const,observacoes:"Bloqueio demonstrativo para investigação."},
    {...base,id:"l6",codigo:"GUA-ANT-01",itemId:"i1",tipo:"Planta seca" as const,origem:"Produção interna CREMIC" as const,recebimento:date(-900),validade:date(-300),qualidade:"Não se aplica" as const,situacao:"Arquivado" as const,finalizadoEm:date(-250),motivoFinalizacao:"Registro histórico anterior ao protótipo"},
  ];
  const movimentos=[movement("m1","i1","l1",12,"loc1",-40),movement("m2","i2","l2",2,"loc2",-100),movement("m3","i3","l3",800,"loc2",-400),movement("m4","i4","l4",500,"loc3",-12),movement("m5","i5","l5",8,"loc3",-30),movement("m6","i1","l6",3,"loc1",-850)];
  movimentos.push({id:"m7",tipo:"Saída",itemId:"i1",loteId:"l1",quantidade:3,unidade:"kg",origemId:"loc1",motivo:"Uso técnico demonstrativo",observacao:"Sem associação a paciente",responsavel:"Equipe CREMIC",dataHora:new Date(now.getTime()-86400000).toISOString(),situacao:"Confirmada"});
  return {schemaVersion:1,items,locais:[{id:"loc1",nome:"Almoxarifado",descricao:"Estoque principal",ativo:true},{id:"loc2",nome:"Farmácia",ativo:true},{id:"loc3",nome:"Quarentena",ativo:true},{id:"loc4",nome:"Laboratório",ativo:true}],lotes,movimentos};
}
