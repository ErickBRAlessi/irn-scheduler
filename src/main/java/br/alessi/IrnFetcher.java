package br.alessi;

import io.quarkus.scheduler.Scheduled;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
public class IrnFetcher {

    @Inject
    EmailSenderService emailSenderService;

    private WebDriver driver;
    private WebDriverWait wait;
    private Set<String> options = new HashSet<>();
    private int count = 289;
    private final int MAX_COUNT = 288;

    @PostConstruct
    public void init() {
        if(driver != null) {
            driver.quit();
        }
        driver = new HtmlUnitDriver(true);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Scheduled(every = "5m")
    @Blocking
    public void fetch() {
        try {
            driver.get("https://siga.marcacaodeatendimento.pt/Marcacao/Entidades");

            WebElement button = wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[title='IRN Registo']")));
            button.click();

            WebElement categoriaDropdown = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("IdCategoria")));
            Select categoriaSelect = new Select(categoriaDropdown);
            boolean categoriaSelected = false;

            for (WebElement option : categoriaSelect.getOptions()) {
                if (option.getText().equals("Citizen")) {
                    categoriaSelect.selectByVisibleText("Citizen");
                    categoriaSelected = true;
                    break;
                }
            }

            if (!categoriaSelected) {
                emailSenderService.sendEmail(new MailDO("Bug", "Option 'Citizen' not found in 'IdCategoria' dropdown."));
                System.err.println("Option 'Citizen' not found in 'IdCategoria' dropdown.");
                return;
            }

            WebElement subCategoriaDropdown = wait.until(ExpectedConditions.elementToBeClickable(By.id("IdSubcategoria")));
            Select subCategoriaSelect = new Select(subCategoriaDropdown);
            wait.until(ExpectedConditions.visibilityOfAllElements(subCategoriaSelect.getOptions()));
            updateDropdownOptions(subCategoriaSelect);

            if (isResidentCardOptionAvailable(subCategoriaSelect)) {
                emailSenderService.sendEmail(new MailDO("!!!!! Avaliable !!!!!", "<h1>The 'Resident Card' option is available.</h1> <br> https://siga.marcacaodeatendimento.pt/Marcacao/Entidades"));
                System.out.println("The 'Resident Card' option is available.");
            } else {
                count++;
                if(count > MAX_COUNT) {
                    StringBuilder report = new StringBuilder("<h1>The 'Resident Card' option is not available.</h1>");
                    this.options.forEach(option -> report.append("<br>").append(option));
                    emailSenderService.sendEmail(new MailDO("Not Avaliable", report.toString()));
                    count = 0;
                }
                System.out.println("The 'Resident Card' option is NOT available.");
            }
        } catch (Exception e) {
            System.err.println("An error occurred during the fetch process: " + e.getMessage());
            init();
        }
    }

    private void updateDropdownOptions(Select select) {
        List<WebElement> options = select.getOptions();
        System.out.println("Options in the dropdown:");
        for (WebElement option : options) {
            this.options.add(option.getText());
            System.out.println(option.getText());
        }
    }

    private boolean isResidentCardOptionAvailable(Select select) {
        List<WebElement> options = select.getOptions();
        for (WebElement option : options) {
            if (option.getText().toLowerCase().contains("resid")) {
                return true;
            }
        }
        return false;
    }
}
