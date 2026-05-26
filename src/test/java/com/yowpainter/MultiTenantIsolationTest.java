package com.yowpainter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.AuthResponse;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.LoginRequest;
import com.yowpainter.modules.auth.infrastructure.adapter.in.web.dto.RegisterRequest;
import com.yowpainter.modules.auth.domain.model.UserRole;
import com.yowpainter.modules.artwork.infrastructure.adapter.in.web.dto.ArtworkCreateRequest;
import com.yowpainter.modules.artwork.domain.model.ArtworkTechnique;
import com.yowpainter.modules.artwork.domain.model.ArtworkStyle;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJson;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureJson
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MultiTenantIsolationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;



    private static String tokenArtistA;
    private static String slugArtistA;
    private static String slugArtistB;

    @Test
    @Order(1)
    void shouldRegisterTwoArtists() throws Exception {
        // 1. Inscrire Artiste A
        RegisterRequest regA = RegisterRequest.builder()
                .firstName("Artist").lastName("Alpha").email("artist.a@example.com")
                .password("Password123").role(UserRole.ROLE_ARTIST).artistName("BoutiqueAlpha").build();
        
        MvcResult resA = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regA)))
                .andExpect(status().isCreated())
                .andReturn();
        
        slugArtistA = objectMapper.readValue(resA.getResponse().getContentAsString(), AuthResponse.class).getTenantId();

        // 2. Inscrire Artiste B
        RegisterRequest regB = RegisterRequest.builder()
                .firstName("Artist").lastName("Beta").email("artist.b@example.com")
                .password("Password123").role(UserRole.ROLE_ARTIST).artistName("BoutiqueBeta").build();
        
        MvcResult resB = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regB)))
                .andExpect(status().isCreated())
                .andReturn();
        
        slugArtistB = objectMapper.readValue(resB.getResponse().getContentAsString(), AuthResponse.class).getTenantId();

        // 3. Connecter Artiste A pour recupérer le token
        LoginRequest loginA = new LoginRequest();
        loginA.setEmail("artist.a@example.com");
        loginA.setPassword("Password123");

        MvcResult resLoginA = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginA)))
                .andExpect(status().isOk())
                .andReturn();
        
        tokenArtistA = objectMapper.readValue(resLoginA.getResponse().getContentAsString(), AuthResponse.class).getAccessToken();
    }

    @Test
    @Order(2)
    void shouldVerifyDataIsolation() throws Exception {
        // 1. Artiste A crée une oeuvre
        ArtworkCreateRequest creation = ArtworkCreateRequest.builder()
                .title("Chef d'oeuvre Unique de A")
                .description("Inaccessible pour B")
                .technique(ArtworkTechnique.OIL)
                .style(ArtworkStyle.ABSTRACT)
                .imageUrls(java.util.List.of("http://example.com/a.jpg"))
                .build();

        MvcResult resCreation = mockMvc.perform(post("/api/artworks")
                .header("Authorization", "Bearer " + tokenArtistA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creation)))
                .andExpect(status().isCreated())
                .andReturn();
        
        String artworkId = JsonPath.read(resCreation.getResponse().getContentAsString(), "$.id");

        // 1b. Passer l'oeuvre en PUBLISHED pour qu'elle soit visible publiquement
        mockMvc.perform(patch("/api/artworks/" + artworkId + "/status")
                .header("Authorization", "Bearer " + tokenArtistA)
                .param("status", "PUBLISHED"))
                .andExpect(status().isOk());

        // 2. Un public consulte la boutique de A -> Doit voir l'oeuvre
        mockMvc.perform(get("/api/v1/public/" + slugArtistA + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("Chef d'oeuvre Unique de A"));

        // 3. Un public consulte la boutique de B -> Doit voir 0 oeuvres (ISOLATION !)
        mockMvc.perform(get("/api/v1/public/" + slugArtistB + "/artworks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
