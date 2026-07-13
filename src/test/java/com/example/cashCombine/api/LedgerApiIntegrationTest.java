package com.example.cashCombine.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LedgerApiIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void createImportListAndChangeCategory() throws Exception {
		MvcResult categoryResult = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Groceries\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Groceries"))
				.andReturn();
		String groceriesId = readJsonField(categoryResult, "id");

		mockMvc.perform(post("/api/rules")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"pattern\":\"WOOLWORTHS\",\"categoryId\":\"" + groceriesId + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.pattern").value("WOOLWORTHS"));

		MvcResult accountResult = mockMvc.perform(post("/api/accounts")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Everyday\",\"type\":\"COMMBANK\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.name").value("Everyday"))
				.andExpect(jsonPath("$.type").value("COMMBANK"))
				.andReturn();
		String accountId = readJsonField(accountResult, "id");

		byte[] csv = """
				10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"
				10/07/2026,"-12.50","CAFE EXAMPLE BLEND FAKETOWN AUS","+2467.50"
				""".getBytes(StandardCharsets.UTF_8);
		MockMultipartFile file = new MockMultipartFile("file", "sample.csv", "text/csv", csv);

		mockMvc.perform(multipart("/api/accounts/" + accountId + "/import").file(file))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accepted").value(2))
				.andExpect(jsonPath("$.duplicate").value(0))
				.andExpect(jsonPath("$.rejected").value(0));

		MvcResult txResult = mockMvc.perform(get("/api/accounts/" + accountId + "/transactions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andReturn();

		String body = txResult.getResponse().getContentAsString();
		assertThat(body).contains("WOOLWORTHS");
		assertThat(body).contains(groceriesId);

		String cafeTxId = extractTransactionIdForDescription(body, "CAFE EXAMPLE");
		MvcResult diningResult = mockMvc.perform(post("/api/categories")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Dining\"}"))
				.andExpect(status().isCreated())
				.andReturn();
		String diningId = readJsonField(diningResult, "id");

		mockMvc.perform(patch("/api/transactions/" + cafeTxId + "/category")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"categoryId\":\"" + diningId + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.categoryId").value(diningId))
				.andExpect(jsonPath("$.categoryAssignmentSource").value("MANUAL"));

		mockMvc.perform(get("/api/accounts"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(accountId));

		mockMvc.perform(delete("/api/accounts/" + accountId)).andExpect(status().isNoContent());
		mockMvc.perform(get("/api/accounts/" + accountId)).andExpect(status().isNotFound());
	}

	private static String readJsonField(MvcResult result, String field) throws Exception {
		String json = result.getResponse().getContentAsString();
		String marker = "\"" + field + "\":\"";
		int start = json.indexOf(marker) + marker.length();
		int end = json.indexOf('"', start);
		return json.substring(start, end);
	}

	private static String extractTransactionIdForDescription(String jsonArray, String descriptionFragment) {
		int descIndex = jsonArray.indexOf(descriptionFragment);
		assertThat(descIndex).isPositive();
		int objectStart = jsonArray.lastIndexOf('{', descIndex);
		String marker = "\"id\":\"";
		int idStart = jsonArray.indexOf(marker, objectStart) + marker.length();
		int idEnd = jsonArray.indexOf('"', idStart);
		return jsonArray.substring(idStart, idEnd);
	}

}
