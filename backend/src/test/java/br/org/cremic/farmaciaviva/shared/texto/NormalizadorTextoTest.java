package br.org.cremic.farmaciaviva.shared.texto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NormalizadorTextoTest {

    @Test
    @DisplayName("nome cientifico com espacos duplos e ponto deve colidir com a forma em maiusculas")
    void formasDiferentesDoMesmoNomeCientificoDevemColidir() {
        String a = NormalizadorTexto.normalizar("Mikania glomerata  Spreng.");
        String b = NormalizadorTexto.normalizar("MIKANIA GLOMERATA SPRENG.");
        String c = NormalizadorTexto.normalizar("  mikania   glomerata spreng ");

        assertThat(a).isEqualTo("MIKANIA GLOMERATA SPRENG");
        assertThat(b).isEqualTo(a);
        assertThat(c).isEqualTo(a);
    }

    @Test
    @DisplayName("acentos devem ser removidos e hifen deve virar espaco")
    void acentosEHifenDevemSerNormalizados() {
        assertThat(NormalizadorTexto.normalizar("Erva-cidreira")).isEqualTo("ERVA CIDREIRA");
        assertThat(NormalizadorTexto.normalizar("erva cidreira")).isEqualTo("ERVA CIDREIRA");
        assertThat(NormalizadorTexto.normalizar("Salva-limão")).isEqualTo("SALVA LIMAO");
        assertThat(NormalizadorTexto.normalizar("Cipó-caatinga")).isEqualTo("CIPO CAATINGA");
    }

    @Test
    @DisplayName("pontuacao de autoria botanica deve ser removida sem juntar palavras")
    void pontuacaoDeveSerRemovida() {
        assertThat(NormalizadorTexto.normalizar("Maytenus ilicifolia Mart. ex Reissek"))
            .isEqualTo("MAYTENUS ILICIFOLIA MART EX REISSEK");
        assertThat(NormalizadorTexto.normalizar("Lippia alba (Mill.) N.E.Br. ex Britton & P.Wilson"))
            .isEqualTo("LIPPIA ALBA MILL NEBR EX BRITTON & PWILSON");
    }

    @Test
    @DisplayName("nomes diferentes nao devem colidir")
    void nomesDiferentesNaoDevemColidir() {
        assertThat(NormalizadorTexto.normalizar("Mikania glomerata"))
            .isNotEqualTo(NormalizadorTexto.normalizar("Mikania laevigata"));
    }

    @Test
    @DisplayName("nulo devolve nulo e texto em branco devolve string vazia")
    void nuloEBranco() {
        assertThat(NormalizadorTexto.normalizar(null)).isNull();
        assertThat(NormalizadorTexto.normalizar("   ")).isEmpty();
    }

    @Test
    @DisplayName("emBrancoParaNulo deve aparar e converter branco em nulo")
    void emBrancoParaNulo() {
        assertThat(NormalizadorTexto.emBrancoParaNulo(null)).isNull();
        assertThat(NormalizadorTexto.emBrancoParaNulo("   ")).isNull();
        assertThat(NormalizadorTexto.emBrancoParaNulo("  Asteraceae ")).isEqualTo("Asteraceae");
    }
}
