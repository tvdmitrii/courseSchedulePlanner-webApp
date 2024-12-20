package com.turygin.servlet.browser;

import com.turygin.api.client.RestClient;
import com.turygin.api.model.CourseDTO;
import com.turygin.api.model.DepartmentDTO;
import com.turygin.states.BrowseCoursesPageState;

import java.io.*;
import java.util.List;

import com.turygin.states.nav.NavigationState;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Main servlet for course browsing. Loads information about courses and departments from REST API.
 * It is also the website home page.
 */
@WebServlet(
        name = "LoadCourses",
        urlPatterns = { "", "/browser/load" }
)
public class LoadCourses extends HttpServlet {

    protected static final Logger LOG = LogManager.getLogger(LoadCourses.class);

    /** Empty constructor. */
    public LoadCourses() {}

    /**
     * Handles HTTP GET requests.
     * @param request object that contains the client's request information
     * @param response object used to send the response back to the client
     * @throws ServletException if servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession();

        // Set navigation state
        NavigationState navState = NavigationState.BROWSER;
        session.setAttribute("navState", navState);

        // Initialize page state
        BrowseCoursesPageState pageState = new BrowseCoursesPageState();

        // Fetch REST API client from the context
        ServletContext context = getServletContext();
        RestClient client = (RestClient) context.getAttribute("restClient");

        // This is a home page, check for authentication error
        String authError = (String) context.getAttribute("authError");
        if (authError != null && !authError.isEmpty()) {
            context.removeAttribute("authError");
            request.setAttribute("error", authError);
        }

        // Fetch department info from the API
        try (Response apiResponse = client.getAllDepartments()) {
            if(RestClient.isStatusSuccess(apiResponse)){
                List<DepartmentDTO> departments = apiResponse.readEntity(new GenericType<>() {});
                pageState.setDepartments(departments);
            } else {
                request.setAttribute("error", RestClient.getErrorMessage(apiResponse));
                forwardToJsp(request, response, navState);
                return;
            }
        }

        // Fetch course info from the API
        try (Response apiResponse = client.getAllCourses()) {
            if(RestClient.isStatusSuccess(apiResponse)){
                List<CourseDTO> courses = apiResponse.readEntity(new GenericType<>() {});
                pageState.setCourses(courses);
            } else {
                request.setAttribute("error", RestClient.getErrorMessage(apiResponse));
                forwardToJsp(request, response, navState);
                return;
            }
        }

        // Save page state in session
        session.setAttribute("browseCoursesPage", pageState);

        forwardToJsp(request, response, navState);
    }

    /**
     * Helper method that forwards to a JSP.
     * @param request client request
     * @param response response object
     * @param navState navigation state object
     * @throws ServletException if servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    private void forwardToJsp(HttpServletRequest request, HttpServletResponse response, NavigationState navState)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = getServletContext().getRequestDispatcher(navState.getJspPage());
        dispatcher.forward(request, response);
    }
}
