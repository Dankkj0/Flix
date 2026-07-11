dependencies {
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}

// Use um número inteiro para as versões. Suba para 2, 3, etc., quando atualizar o código!
version = 1

cloudstream {
    // Breve descrição do que o seu plugin faz
    description = "Provedor de filmes e séries em português usando a fonte do PobreFlix."
    
    // Seu nome como desenvolvedor
    authors = listOf("Dankkj0")

    /**
    * Status:
    * 0: Down / Fora do ar
    * 1: Ok / Funcionando
    * 2: Slow / Lento
    * 3: Beta-only
    **/
    status = 1 

    // Tipos de conteúdo que o seu raspador suporta (Filmes e Séries)
    tvTypes = listOf("Movie", "TvSeries")

    requiresResources = true
    
    // Altere para "pt-BR" para o Cloudstream saber que o conteúdo é em português!
    language = "pt-BR"

    // Você pode colocar a URL de qualquer imagem quadrada aqui para ser o ícone do seu plugin
    iconUrl = "https://upload.wikimedia.org/wikipedia/commons/2/2f/Korduene_Logo.png"
}

android {
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}
