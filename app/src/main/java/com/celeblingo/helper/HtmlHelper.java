package com.celeblingo.helper;

import android.util.Log;

import com.celeblingo.model.Meetings;

import java.util.ArrayList;

public class HtmlHelper {

    public static String createMeetingInfoHTML(ArrayList<Meetings> meetingsArrayList) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < meetingsArrayList.size(); i++) {
            stringBuilder.append("<h1>").append(meetingsArrayList.get(i).getSummary()).append("</h1>")
                    .append("<ul><li><span class=\"material-icons\">question_answer</span>").append("<a href='").append(meetingsArrayList.get(i).getGptUrl()).append("'>")
                    .append("GPT Url").append("</a>").append("</li>")
                    .append("<li><span class=\"material-icons\">cloud</span>").append("<a href='").append(meetingsArrayList.get(i).getDriveUrl()).append("'>")
                    .append("Google Drive Url").append("</a>").append("</li>")
                    .append("<li><span class=\"material-icons\">videocam</span>").append("<a href='").append(meetingsArrayList.get(i).getVideoUrl()).append("'>")
                    .append("Video Url").append("</a>").append("</li>")
                    .append("<li><span class=\"material-icons\">language</span>").append("<a href='").append(meetingsArrayList.get(i).getHtmlUrl()).append("'>")
                    .append("HTML Url").append("</a>").append("</li></ul><br>");
        }
        String meetingsHistory = stringBuilder.toString();
        Log.d("==html", meetingsHistory);
        String htmlContent = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "    <title>Meeting Information</title>" +
                "    <link href=\"https://fonts.googleapis.com/css?family=Roboto:400,500,700&display=swap\" rel=\"stylesheet\">" +
                "    <link href=\"https://fonts.googleapis.com/icon?family=Material+Icons\" rel=\"stylesheet\">" +
                "  <style>" +
                "    body {" +
                "    font-family: 'Roboto', sans-serif;" +
                "    background-color: #F9F9F9;" +
                "    margin: 0;" +
                "    padding: 20px;" +
                "}" +

                ".content {" +
                "    background-color: #FFF;" +
                "    border-radius: 8px;" +
                "    box-shadow: 0 2px 4px rgba(0,0,0,0.1);" +
                "    padding: 20px;" +
                "    max-width: 800px;" +
                "    margin: auto;" +
                "}" +

                "h1 {" +
                "    font-size: 24px;" +
                "    color: #E57373;" +
                "    margin-bottom: 5px;" +
                "}" +

                ".set-list, .meeting-list ul {" +
                "    list-style: none;" +
                "    padding: 0;" +
                "    margin: 0;" +
                "}" +

                ".set-list li, .meeting-list li {" +
                "    background-color: #FFECB3;" +
                "    margin-bottom: 10px;" +
                "    padding: 10px 15px;" +
                "    border-radius: 4px;" +
                "    display: flex;" +
                "    align-items: center;" +
                "}" +

                ".set-list li a, .meeting-list li a {" +
                "    color: #333;" +
                "    text-decoration: none;" +
                "    margin-left: 15px;" +
                "    font-weight: 500;" +
                "}" +

                ".material-icons {" +
                "    color: #4DB6AC;" +
                "    font-size: 24px;" +
                "}" +

                "/* Pagination Styles */" +
                "#pagination {" +
                "    text-align: center;" +
                "    margin-top: 20px;" +
                "}" +

                ".page-number {" +
                "    display: inline-block;" +
                "    padding: 5px 10px;" +
                "    margin-right: 5px;" +
                "    border: 1px solid #ddd;" +
                "    color: #333;" +
                "    cursor: pointer;" +
                "}" +

                ".page-number.active {" +
                "    background-color: #4DB6AC;" +
                "    color: white;" +
                "    border-color: #4DB6AC;" +
                "}" +
                ".title-container {" +
                "    text-align: center;" +
                "    margin-bottom: 20px;" +
                "}" +
                ".page-title {" +
                "    font-size: 28px;" +
                "    color: #4DB6AC; /* This matches your icon color for consistency */" +
                "    font-weight: 700; /* Making the title bolder */" +
                "    margin: 0;" +
                "    padding: 10px 0; /* Adds some spacing above and below the title */" +
                "}" +
                "  </style>" +
                "</head>" +
                "<body>" +
                "<div class=\"title-container\">" +
                "    <h1 class=\"page-title\">Kidi AI CelebLingo</h1>" +
                "</div>" +
                "    <div class=\"content\">" +
                "        <div id=\"meetings\">" +
                "            <!-- Meeting 1 -->" +
                "            <section class=\"meeting-list\">" +
                meetingsHistory +
                "            </section>" +
                "        </div>" +
                "        <div id=\"pagination\"></div>" +
                "    </div>" +
                "    <script>" +
                "  document.addEventListener('DOMContentLoaded', function () {" +
                "    const meetingsPerPage = 5;" +
                "    let currentPage = 1;" +

                "    // Sample data for the meetings" +
                "    const meetings = Array.from({ length: 25 }, (_, i) => {" +
                "        return {" +
                "            title: `Meeting ${i + 1}`," +
                "            links: [" +
                "                { icon: 'event', url: 'https://example.com/meeting_gpt' + (i + 1), text: 'GPT Url' }," +
                "                { icon: 'cloud', url: 'https://example.com/meeting_drive' + (i + 1), text: 'Google Drive Url' }," +
                "                { icon: 'videocam', url: 'https://example.com/meeting_video' + (i + 1), text: 'Video Url' }," +
                "                { icon: 'language', url: 'https://example.com/meeting_html' + (i + 1), text: 'HTML Url' }" +
                "            ]" +
                "        };" +
                "    });" +

                "    function displayMeetings(page) {" +
                "        const start = (page - 1) * meetingsPerPage;" +
                "        const end = start + meetingsPerPage;" +
                "        const meetingsToShow = meetings.slice(start, end);" +

                "        const meetingsContainer = document.getElementById('meetings');" +
                "        meetingsContainer.innerHTML = '';" +

                "        meetingsToShow.forEach(meeting => {" +
                "            const section = document.createElement('section');" +
                "            section.className = 'meeting-list';" +
                "            section.innerHTML = `<h1>${meeting.title}</h1>` +" +
                "                meeting.links.map(link => `<li><span class=\"material-icons\">${link.icon}</span><a href=\"${link.url}\">${link.text}</a></li>`).join('');" +
                "            meetingsContainer.appendChild(section);" +
                "        });" +
                "    }" +

                "    function setupPagination() {" +
                "        const totalPages = Math.ceil(meetings.length / meetingsPerPage);" +
                "        const paginationContainer = document.getElementById('pagination');" +
                "        paginationContainer.innerHTML = '';" +

                "        for (let i = 1; i <= totalPages; i++) {" +
                "            const pageNumber = document.createElement('span');" +
                "            pageNumber.className = `page-number ${currentPage === i ? 'active' : ''}`;" +
                "            pageNumber.textContent =" +
                "document.addEventListener('DOMContentLoaded', function () {" +
                "    const meetingsPerPage = 5;" +
                "    let currentPage = 1;" +
                "    const meetingSections = document.querySelectorAll('.meeting-list');" +
                "    const paginationContainer = document.getElementById('pagination');" +

                "    function displayPage(page) {" +
                "        const start = (page - 1) * meetingsPerPage;" +
                "        const end = start + meetingsPerPage;" +
                "        meetingSections.forEach((section, index) => {" +
                "            if (index >= start && index < end) {" +
                "                section.style.display = 'block';" +
                "            } else {" +
                "                section.style.display = 'none';" +
                "            }" +
                "        });" +
                "        setupPagination(page);" +
                "    }" +

                "    function setupPagination(selectedPage) {" +
                "        paginationContainer.innerHTML = '';" +
                "        const totalPages = Math.ceil(meetingSections.length / meetingsPerPage);" +
                "        " +
                "        for (let i = 1; i <= totalPages; i++) {" +
                "            const pageNumber = document.createElement('span');" +
                "            pageNumber.className = `page-number ${selectedPage === i ? 'active' : ''}`;" +
                "            pageNumber.textContent = i;" +
                "            pageNumber.addEventListener('click', () => displayPage(i));" +
                "            paginationContainer.appendChild(pageNumber);" +
                "        }" +
                "    }" +

                "    displayPage(currentPage);" +
                "});" +
                "</script>" +
                "</body>" +
                "</html>";

        return htmlContent;
    }


    public static String createChatDataHtml(String summary, String modifiedValue, String allImageUrl) {
        String html = "<!DOCTYPE html>" +
                "<html lang=\"en\">" +
                "<head>" +
                "    <meta charset=\"UTF-8\">" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">" +
                "<h1 id=\"page-title\" class=\"page-title\">" + summary + "</h1>" +
                "  " +
                "  <style>" +
                "  @import url('https://fonts.googleapis.com/css2?family=Rubik:wght@400;500;700&display=swap');" +
                "* {" +
                "    margin: 0;" +
                "    padding: 0;" +
                "    box-sizing: border-box;" +
                "}" +
                "body {" +
                "    font-family: 'Rubik', sans-serif;" +
                "    background-color: #fff;" +
                "    color: #333;" +
                "    line-height: 1.6;" +
                "    padding: 20px;" +
                "}" +
                ".chat-container {" +
                "    margin: 30px auto;" +
                "    padding: 20px;" +
                "    background-color: #f7f7f7;" +
                "    border-radius: 16px;" +
                "    box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);" +
                "}" +
                ".caption {" +
                "    background-color: rgba(0, 0, 0, 0.5);" +
                "    color: #fff;" +
                "    padding: 10px 20px;" +
                "    font-size: 3em; /* Adjust font size as needed */" +
                "    position: absolute;" +
                "    bottom: 10px; /* Adjust as necessary */" +
                "    left: 50%;" +
                "    transform: translateX(-50%); /* Center the caption */" +
                "    max-width: 80%; /* Max width of text bubble */" +
                "    border-radius: 25px; /* Rounded corners for the text bubble */" +
                "    text-align: center;" +
                "    box-sizing: border-box; /* Include padding in the width */" +
                "    line-height: 1.4;" +
                "    white-space: normal; /* Allows text wrapping */" +
                "    overflow: hidden;" +
                "    display: flex;" +
                "    justify-content: center;" +
                "    align-items: center;" +
                "    min-height: 40px; /* Ensure bubble size for short texts */" +
                "}" +
                ".image-container {" +
                "    position: relative;" +
                "    margin: 20px 0;" +
                "    overflow: hidden; /* Ensures the border radius applies to children */" +
                "}" +
                ".caption {" +
                "  font-family: 'Rubik', sans-serif;  " +
                "  position: absolute;" +
                "    bottom: 10px;" +
                "    left: 50%;" +
                "    transform: translateX(-50%);" +
                "    /* The rest of your .caption styles remain unchanged */" +
                "}" +
                ".chat-message {" +
                "    text-align: center;" +
                "    display: block;" +
                "    margin: auto;" +
                "    width: 80%; /* or your preferred width */" +
                "}" +
                ".image-container {" +
                "    display: flex;" +
                "    justify-content: center;" +
                "    margin: 20px 0; /* Keeps your existing margin */" +
                "}" +
                ".image-container img {" +
                "    width: 100%; /* Make the image fill the container */" +
                "    display: block; /* Removes extra space below the image */" +
                "}" +
                ".caption {" +
                "    width: 80%; /* This is now 80% of the image-container */" +
                "    /* The rest of your .caption styles remain unchanged */" +
                "}" +
                ".print-button {" +
                "    display: block;" +
                "    width: auto;" +
                "    padding: 10px 20px;" +
                "    margin: 10px auto;" +
                "    background-color: #4CAF50; /* Example color */" +
                "    color: white;" +
                "    border: none;" +
                "    border-radius: 5px;" +
                "    cursor: pointer;" +
                "    font-size: 16px;" +
                "    text-align: center;" +
                "}" +
                "@media print {" +
                "    .print-button {" +
                "        display: none;" +
                "    }" +
                "    /* Add any other print-specific styles here */" +
                "}" +
                "@media print {" +
                "  body {" +
                "    -webkit-print-color-adjust: exact;" +
                "    color-adjust: exact;" +
                "  }" +
                "  .caption {" +
                "    /* Force the print color to match the screen color */" +
                "    background-color: rgba(0, 0, 0, 0.5) !important;" +
                "    color: #fff !important;" +
                "    font-size: 3em; /* Maintain the same size as on screen */" +
                "    /* More styles here */" +
                "  }" +
                "  .print-button {" +
                "    display: none;" +
                "  }" +
                "  /* Add other print-specific styles here */" +
                "}" +
                "@media print {" +
                "    body, html {" +
                "        margin: 0;" +
                "        padding: 0;" +
                "        height: 100%;" +
                "        width: 100%;" +
                "    }" +
                "    .image-container {" +
                "        width: 297mm; /* A4 width */" +
                "        height: 210mm; /* A4 height, which is the width in landscape */" +
                "        position: relative;" +
                "        page-break-after: always; /* Each image on a new page */" +
                "        page-break-inside: avoid; /* Avoid breaking inside the image */" +
                "    }" +
                "    .image-container img {" +
                "        position: absolute;" +
                "        width: auto; /* Auto width to maintain aspect ratio */" +
                "        height: 100%;" +
                "        left: 50%;" +
                "        transform: translateX(-50%); /* Center horizontally */" +
                "        object-fit: cover; /* Crop sides to maintain aspect ratio */" +
                "    }" +
                "    .caption," +
                "    .chat-container," +
                "    .print-button {" +
                "        display: none; /* Hide elements not needed for printing */" +
                "    }" +
                "}" +
                "</style>" +
                "</head>" +
                "<body>" +
                "    <div class=\"chat-container\">" +
                "        <div class=\"chat-message system\">" + modifiedValue + "</div>" +
                "    </div>" +
                "<button onclick=\"window.print();\" class=\"print-button\">Print this book</button>" +

                allImageUrl +

                "    <script>" +
                "  document.addEventListener('DOMContentLoaded', function() {" +
                "    var pageTitle = document.getElementById('page-title').textContent;" +
                "    var chatMessageHTML = document.querySelector('.chat-message.system').innerHTML;" +
                "    var sentences = [...chatMessageHTML.matchAll(/#([^<]+)(?=<br>|<\\/div>)/g)].map(match => match[1].trim());" +
                "    var captions = document.querySelectorAll('.image-container .caption');" +
                "    captions.forEach((caption, index) => {" +
                "        if (index === 0) {" +
                "            caption.innerHTML = pageTitle;" +
                "            caption.classList.add('first-caption');" +
                "            caption.style.display = 'flex';" +
                "        } else {" +
                "            var sentenceIndex = index - 1;" +
                "            if (sentences[sentenceIndex]) {" +
                "                caption.innerHTML = sentences[sentenceIndex];" +
                "                caption.style.display = 'flex';" +
                "            }" +
                "        }" +
                "    });" +
                "});" +
                "</script>" +
                "</body>" +
                "</html>";
        return html;
    }

}
