/* $This file is distributed under the terms of the license in LICENSE$ */

package edu.cornell.mannlib.vitro.webapp.dynapi;

import static edu.cornell.mannlib.vitro.webapp.dynapi.request.ApiRequestPath.REST_SERVLET_PATH;
import static java.lang.String.format;
import static javax.servlet.http.HttpServletResponse.SC_METHOD_NOT_ALLOWED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.cornell.mannlib.vitro.webapp.beans.UserAccount;
import edu.cornell.mannlib.vitro.webapp.dynapi.data.implementation.DynapiModelFactory;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

@RunWith(Parameterized.class)
public class RESTEndpointIntegrationTest extends AbstractIntegrationTest {

    private final static String BASE_URL = "http://localhost:8080";

    private final static String MOCK_BASE_PATH = "src/test/resources/dynapi/mock";

    private final static boolean overwrite = false;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RESTEndpoint restEndpoint;

    private ProcedurePool procedurePool;

    private ResourceAPIPool resourceAPIPool;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @Mock
    private UserAccount user;

    @Mock
    private PrintWriter responsePrintWriter;

    @Parameter(0)
    public String testRequestMethod;

    @Parameter(1)
    public String testRequestPath;

    @Parameter(2)
    public String testRequestParamsFile;

    @Parameter(3)
    public String testRequestBodyFile;

    @Parameter(4)
    public Integer testResponseStatus;

    @Parameter(5)
    public String testResponseBodyFile;

    @Parameter(6)
    public String testMessage;

    private static MockedStatic<DynapiModelFactory> dynapiModelFactory;

    @BeforeClass
    public static void setupStaticObjects() {
        dynapiModelFactory = mockStatic(DynapiModelFactory.class);
    }

    @AfterClass
    public static void after() {
        dynapiModelFactory.close();
    }

    @Before
    public void beforeEach() throws IOException {
        LoggingControl.offLogs();
        procedurePool = ProcedurePool.getInstance();
        resourceAPIPool = ResourceAPIPool.getInstance();

        restEndpoint = new RESTEndpoint();

        loadDefaultModel();
        loadTestModel();

        procedurePool.init();
        procedurePool.reload();

        resourceAPIPool.init();
        resourceAPIPool.reload();

        MockitoAnnotations.openMocks(this);

        dynapiModelFactory.when(() -> DynapiModelFactory.getModel(any(String.class))).thenReturn(ontModel);
        mockStatus(response);
    }

    @After
    public void reset() {
        LoggingControl.restoreLogs();
    }

    @Test
    public void doTest() throws IOException {
        when(request.getMethod()).thenReturn(testRequestMethod);
        when(request.getRequestURL()).thenReturn(new StringBuffer(BASE_URL + REST_SERVLET_PATH + testRequestPath));
        when(request.getRequestURI()).thenReturn(BASE_URL + REST_SERVLET_PATH + testRequestPath);
        when(request.getServletPath()).thenReturn(REST_SERVLET_PATH);
        when(request.getPathInfo()).thenReturn(testRequestPath);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("user")).thenReturn(user);
        when(user.isRootUser()).thenReturn(true);

        when(request.getHeader(HttpHeaders.ACCEPT)).thenReturn(ContentType.APPLICATION_JSON.toString());

        if (testRequestParamsFile != null) {
            String filePath = format("%s/rest/request/params/%s/%s", MOCK_BASE_PATH, testRequestMethod.toLowerCase(),
                    testRequestParamsFile);
            Map<String, String[]> params =
                    objectMapper.readValue(new File(filePath), new TypeReference<HashMap<String, String[]>>() {
                    });
            when(request.getParameterMap()).thenReturn(params);
        }

        if (testRequestBodyFile != null) {
            String filePath = format("%s/rest/request/body/%s/%s", MOCK_BASE_PATH, testRequestMethod.toLowerCase(),
                    testRequestParamsFile);
            when(request.getContentType()).thenReturn("application/json;UTF-8");
            when(request.getReader()).thenReturn(new BufferedReader(new FileReader(filePath)));
        }

        if (testResponseBodyFile != null) {
            when(response.getWriter()).thenReturn(responsePrintWriter);
        }

        run(testRequestMethod);

        if (testResponseStatus != null) {
            verify(response, times(1)).setStatus(testResponseStatus);

            if (testResponseBodyFile != null) {
                String filePath =
                        format("rest/response/body/%s/%s", testRequestMethod.toLowerCase(), testRequestParamsFile);
                String expectedReponseBody = readMockFile(filePath);
                if (overwrite) {
                    overwrite(filePath);
                }
                verify(responsePrintWriter, times(1)).print(expectedReponseBody.trim());
                verify(responsePrintWriter, times(1)).flush();
            }

        } else {
            verify(response, times(0)).setStatus(anyInt());
        }

        assertEquals("Invalid Status for test: " + testMessage, status, response.getStatus());
    }

    private void overwrite(String filePath) throws FileNotFoundException {
        final ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(responsePrintWriter).print(captor.capture());
        final String argument = captor.getValue();
        PrintWriter out = new PrintWriter(getTestFilePath(filePath));
        out.print(argument);
        out.flush();
    }

    private void run(String method) {
        switch (method) {
            case "POST":
                restEndpoint.doPost(request, response);
                break;
            case "GET":
                restEndpoint.doGet(request, response);
                break;
            case "PUT":
                restEndpoint.doPut(request, response);
                break;
            case "PATCH":
                restEndpoint.doPatch(request, response);
                break;
            case "DELETE":
                restEndpoint.doDelete(request, response);
                break;
            default:
                break;
        }
    }

    @Parameterized.Parameters
    public static Collection<Object[]> requests() {
        List<Object[]> requests = new ArrayList<>(Arrays.asList(new Object[][] {
            { "POST",   "/1/test_not_found", null, null, SC_NOT_FOUND,          null,
                "Resource not found" },
            { "GET",    "/1/test_not_found", null, null, SC_NOT_FOUND,          null,
                "Resource not found" },
            { "PUT",    "/1/test_not_found", null, null, SC_METHOD_NOT_ALLOWED, null,
                "Method not allowed before looking for resource" },
            { "PATCH",  "/1/test_not_found", null, null, SC_METHOD_NOT_ALLOWED, null,
                "Method not allowed before looking for resource" },
            { "DELETE", "/1/test_not_found", null, null, SC_METHOD_NOT_ALLOWED, null,
                "Method not allowed before looking for resource" }
        }));

        // update with applicable individual ids, ideally they would exist in model for integration testing
        requests.addAll(requests("test_collection_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_collection_custom_action_name"));
        requests.addAll(requests("test_concept_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_concept_custom_action_name"));
        requests.addAll(requests("test_document_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_document_custom_action_name"));
        requests.addAll(requests("test_organization_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_organization_custom_action_name"));
        requests.addAll(requests("test_person_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_person_custom_action_name"));
        requests.addAll(requests("test_process_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_process_custom_action_name"));
        requests.addAll(requests("test_relationship_resource",
                "https://scholars.institution.edu/individual/n1f9d4ddc", "test_relationship_custom_action_name"));

        return requests;
    }

    public static List<Object[]> requests(String resourceName, String individualUri, String customRestActionName) {
        // TODO: create appropriate request body, request params, and response body files when applicable
        // String restReqParamsFile = format("%s.json", resourceName);
        // String restReqBodyFile = format("%s.json", resourceName);
        // String restResBodyFile = format("%s.json", resourceName);
        String restReqParamsFile = "test_collection_resource.json";
        String restReqBodyFile = "test_collection_resource.json";
        String restResBodyFile = "test_collection_resource.json";

        String resourcePath = format("/1/%s", resourceName);

        String individualResourcePath = format("%s/resource:%s", resourcePath,
                new String(Base64.getEncoder().encode(individualUri.getBytes())));

        String customRestActionPath = format("%s/%s", individualResourcePath, customRestActionName);

        return new ArrayList<>(Arrays.asList(new Object[][] {
            { "POST",   resourcePath,           restReqParamsFile, restReqBodyFile,
                SC_OK,                 restResBodyFile, "Create collection resource" },
            { "GET",    resourcePath,           restReqParamsFile, null,
                SC_OK,                 restResBodyFile, "Get collection resources" },
            { "PUT",    resourcePath,           restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Update not allowed on collection" },
            { "PATCH",  resourcePath,           restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Patch not allowed on collection" },
            { "DELETE", resourcePath,           restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Delete not allowed on collection" },

            { "POST",   individualResourcePath, restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Create not allowed on individualt resource" },
            { "GET",    individualResourcePath, restReqParamsFile, null,
                SC_OK,                 restResBodyFile, "Get individual resource" },
            { "PUT",    individualResourcePath, restReqParamsFile, restReqBodyFile,
                SC_OK,                 restResBodyFile, "Update individual resource" },
            { "PATCH",  individualResourcePath, restReqParamsFile, restReqBodyFile,
                SC_OK,                 restResBodyFile, "Patch individual resource" },
            { "DELETE", individualResourcePath, restReqParamsFile, null,
                SC_OK,                 restResBodyFile, "Delete individual resource" },

            { "POST",   customRestActionPath,   restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Method unsupported by Custom REST action" },
            { "GET",    customRestActionPath,   restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Method unsupported by Custom REST action" },
            { "PUT",    customRestActionPath,   restReqParamsFile, restReqBodyFile,
                SC_OK,                 restResBodyFile, "Run Custom REST action" },
            { "PATCH",  customRestActionPath,   restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Method unsupported by Custom REST action" },
            { "DELETE", customRestActionPath,   restReqParamsFile, null,
                SC_METHOD_NOT_ALLOWED, null,            "Method unsupported by Custom REST action" }
        }));
    }

}
