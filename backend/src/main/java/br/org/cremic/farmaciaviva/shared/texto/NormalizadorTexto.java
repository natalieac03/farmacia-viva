package br.org.cremic.farmaciaviva.shared.texto;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Normalizacao de texto para comparacao e unicidade.
 *
 * Produz a forma canonica usada nas colunas "*_normalizado": dois textos que
 * uma pessoa consideraria "o mesmo nome" devem colidir aqui e, por
 * consequencia, no indice unico do banco.
 *
 * Regras, nesta ordem:
 *   1. remove acentos e demais marcas diacriticas (decomposicao NFD);
 *   2. hifen, sublinhado e barra viram espaco ("erva-cidreira" = "erva cidreira");
 *   3. pontuacao comum e removida (ponto, virgula, ponto e virgula, dois
 *      pontos, aspas, parenteses, colchetes, exclamacao, interrogacao,
 *      porcentagem), para que "Spreng." e "Spreng" colidam;
 *   4. espacos multiplos sao colapsados em um e as pontas sao aparadas;
 *   5. tudo em maiusculas, com Locale.ROOT para nao depender do sistema.
 *
 * E deliberadamente uma classe utilitaria estatica e sem estado: modulos
 * futuros (item, categoria, localizacao) devem reutilizar exatamente esta
 * regra em vez de reescrever a sua.
 */
public final class NormalizadorTexto {

    private static final Pattern DIACRITICOS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARADORES = Pattern.compile("[-_/]+");
    private static final Pattern PONTUACAO = Pattern.compile("[.,;:'\"()\\[\\]!?%]+");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private NormalizadorTexto() {
    }

    /**
     * @return a forma normalizada, ou {@code null} quando a entrada e nula.
     *         Uma entrada em branco resulta em string vazia.
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String semAcentos = DIACRITICOS
            .matcher(Normalizer.normalize(texto, Normalizer.Form.NFD))
            .replaceAll("");
        String comEspacos = SEPARADORES.matcher(semAcentos).replaceAll(" ");
        String semPontuacao = PONTUACAO.matcher(comEspacos).replaceAll("");
        String colapsado = ESPACOS.matcher(semPontuacao).replaceAll(" ").trim();
        return colapsado.toUpperCase(Locale.ROOT);
    }

    /**
     * Apara o texto e devolve {@code null} quando ele e nulo ou fica em branco.
     * Uso: campos opcionais que nao devem ser gravados como string vazia.
     */
    public static String emBrancoParaNulo(String texto) {
        if (texto == null) {
            return null;
        }
        String aparado = texto.trim();
        return aparado.isEmpty() ? null : aparado;
    }
}
