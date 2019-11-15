package uk.gov.pay.connector.it.dao;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.gov.pay.connector.cardtype.model.domain.CardTypeEntity;
import uk.gov.pay.connector.gatewayaccount.dao.GatewayAccountDao;
import uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity;
import uk.gov.pay.connector.usernotification.model.domain.NotificationCredentials;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static org.apache.commons.lang.math.RandomUtils.nextLong;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static uk.gov.pay.connector.gatewayaccount.model.GatewayAccountEntity.Type.TEST;
import static uk.gov.pay.connector.util.AddGatewayAccountParams.AddGatewayAccountParamsBuilder.anAddGatewayAccountParams;

public class GatewayAccountDaoIT extends DaoITestBase {

    private GatewayAccountDao gatewayAccountDao;
    private DatabaseFixtures databaseFixtures;
    private long gatewayAccountId;

    @Before
    public void setUp() {
        gatewayAccountDao = env.getInstance(GatewayAccountDao.class);
        databaseFixtures = DatabaseFixtures.withDatabaseTestHelper(databaseTestHelper);
        gatewayAccountId = nextLong();
    }

    @After
    public void truncate() {
        databaseTestHelper.truncateAllData();
    }

    @Test
    public void persist_shouldCreateAnAccount() {
        final CardTypeEntity masterCardCredit = databaseTestHelper.getMastercardCreditCard();
        final CardTypeEntity visaCardDebit = databaseTestHelper.getVisaCreditCard();

        createAccountRecordWithCards();

        String paymentProvider = "test provider";
        GatewayAccountEntity account = new GatewayAccountEntity(paymentProvider, new HashMap<>(), TEST);

        account.setCardTypes(Arrays.asList(masterCardCredit, visaCardDebit));

        gatewayAccountDao.persist(account);

        assertThat(account.getId(), is(notNullValue()));
        assertThat(account.getEmailNotifications().isEmpty(), is(true));
        assertThat(account.getDescription(), is(nullValue()));
        assertThat(account.getAnalyticsId(), is(nullValue()));
        assertThat(account.getNotificationCredentials(), is(nullValue()));
        assertThat(account.getCorporateNonPrepaidCreditCardSurchargeAmount(), is(0L));
        assertThat(account.getCorporateNonPrepaidDebitCardSurchargeAmount(), is(0L));
        assertThat(account.getCorporatePrepaidCreditCardSurchargeAmount(), is(0L));
        assertThat(account.getCorporatePrepaidDebitCardSurchargeAmount(), is(0L));
        assertThat(account.isSendPayerIpAddressToGateway(), is(false));

        databaseTestHelper.getAccountCredentials(account.getId());

        List<Map<String, Object>> acceptedCardTypesByAccountId = databaseTestHelper.getAcceptedCardTypesByAccountId(account.getId());

        assertThat(acceptedCardTypesByAccountId, containsInAnyOrder(
                allOf(
                        hasEntry("label", masterCardCredit.getLabel()),
                        hasEntry("type", masterCardCredit.getType().toString()),
                        hasEntry("brand", masterCardCredit.getBrand())
                ), allOf(
                        hasEntry("label", visaCardDebit.getLabel()),
                        hasEntry("type", visaCardDebit.getType().toString()),
                        hasEntry("brand", visaCardDebit.getBrand())
                )));
    }

    @Test
    public void findById_shouldNotFindANonexistentGatewayAccount() {
        assertThat(gatewayAccountDao.findById(GatewayAccountEntity.class, 1234L).isPresent(), is(false));
    }

    @Test
    public void findById_shouldFindGatewayAccount() {
        final CardTypeEntity masterCardCredit = databaseTestHelper.getMastercardCreditCard();
        final CardTypeEntity visaCardDebit = databaseTestHelper.getVisaCreditCard();
        DatabaseFixtures.TestAccount accountRecord = createAccountRecordWithCards(masterCardCredit, visaCardDebit);

        Optional<GatewayAccountEntity> gatewayAccountOpt =
                gatewayAccountDao.findById(GatewayAccountEntity.class, accountRecord.getAccountId());

        assertTrue(gatewayAccountOpt.isPresent());
        GatewayAccountEntity gatewayAccount = gatewayAccountOpt.get();
        assertThat(gatewayAccount.getGatewayName(), is(accountRecord.getPaymentProvider()));
        Map<String, String> credentialsMap = gatewayAccount.getCredentials();
        assertThat(credentialsMap.size(), is(0));
        assertThat(gatewayAccount.getServiceName(), is(accountRecord.getServiceName()));
        assertThat(gatewayAccount.getDescription(), is(accountRecord.getDescription()));
        assertThat(gatewayAccount.getAnalyticsId(), is(accountRecord.getAnalyticsId()));
        assertThat(gatewayAccount.getCorporateNonPrepaidCreditCardSurchargeAmount(), is(accountRecord.getCorporateCreditCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporateNonPrepaidDebitCardSurchargeAmount(), is(accountRecord.getCorporateDebitCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporatePrepaidCreditCardSurchargeAmount(), is(accountRecord.getCorporatePrepaidCreditCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporatePrepaidDebitCardSurchargeAmount(), is(accountRecord.getCorporatePrepaidDebitCardSurchargeAmount()));
        assertThat(gatewayAccount.getCardTypes(), contains(
                allOf(
                        hasProperty("id", is(Matchers.notNullValue())),
                        hasProperty("label", is(masterCardCredit.getLabel())),
                        hasProperty("type", is(masterCardCredit.getType())),
                        hasProperty("brand", is(masterCardCredit.getBrand()))
                ), allOf(
                        hasProperty("id", is(Matchers.notNullValue())),
                        hasProperty("label", is(visaCardDebit.getLabel())),
                        hasProperty("type", is(visaCardDebit.getType())),
                        hasProperty("brand", is(visaCardDebit.getBrand()))
                )));
    }

    @Test
    public void findById_shouldFindGatewayAccountWithCorporateSurcharges() {
        DatabaseFixtures.TestAccount accountRecord = createAccountRecordWithCorporateSurcharges();

        Optional<GatewayAccountEntity> gatewayAccountOpt =
                gatewayAccountDao.findById(GatewayAccountEntity.class, accountRecord.getAccountId());

        assertTrue(gatewayAccountOpt.isPresent());
        GatewayAccountEntity gatewayAccount = gatewayAccountOpt.get();
        assertThat(gatewayAccount.getGatewayName(), is(accountRecord.getPaymentProvider()));
        Map<String, String> credentialsMap = gatewayAccount.getCredentials();
        assertThat(credentialsMap.size(), is(0));
        assertThat(gatewayAccount.getServiceName(), is(accountRecord.getServiceName()));
        assertThat(gatewayAccount.getDescription(), is(accountRecord.getDescription()));
        assertThat(gatewayAccount.getAnalyticsId(), is(accountRecord.getAnalyticsId()));
        assertThat(gatewayAccount.getCorporateNonPrepaidCreditCardSurchargeAmount(), is(accountRecord.getCorporateCreditCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporateNonPrepaidDebitCardSurchargeAmount(), is(accountRecord.getCorporateDebitCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporatePrepaidCreditCardSurchargeAmount(), is(accountRecord.getCorporatePrepaidCreditCardSurchargeAmount()));
        assertThat(gatewayAccount.getCorporatePrepaidDebitCardSurchargeAmount(), is(accountRecord.getCorporatePrepaidDebitCardSurchargeAmount()));
        
    }
    
    @Test
    public void findById_shouldUpdateAccountCardTypes() {
        final CardTypeEntity masterCardCredit = databaseTestHelper.getMastercardCreditCard();
        final CardTypeEntity visaCardCredit = databaseTestHelper.getVisaCreditCard();
        final CardTypeEntity visaCardDebit = databaseTestHelper.getVisaDebitCard();
        DatabaseFixtures.TestAccount accountRecord = createAccountRecordWithCards(masterCardCredit, visaCardCredit);

        Optional<GatewayAccountEntity> gatewayAccountOpt =
                gatewayAccountDao.findById(GatewayAccountEntity.class, accountRecord.getAccountId());

        assertTrue(gatewayAccountOpt.isPresent());
        GatewayAccountEntity gatewayAccount = gatewayAccountOpt.get();

        List<CardTypeEntity> cardTypes = gatewayAccount.getCardTypes();

        cardTypes.removeIf(p -> p.getId().equals(visaCardCredit.getId()));
        cardTypes.add(visaCardDebit);

        gatewayAccountDao.merge(gatewayAccount);

        List<Map<String, Object>> acceptedCardTypesByAccountId = databaseTestHelper.getAcceptedCardTypesByAccountId(accountRecord.getAccountId());

        assertThat(acceptedCardTypesByAccountId, contains(
                allOf(
                        hasEntry("label", masterCardCredit.getLabel()),
                        hasEntry("type", masterCardCredit.getType().toString()),
                        hasEntry("brand", masterCardCredit.getBrand())
                ), allOf(
                        hasEntry("label", visaCardDebit.getLabel()),
                        hasEntry("type", visaCardDebit.getType().toString()),
                        hasEntry("brand", visaCardDebit.getBrand())
                )));
    }

    @Test
    public void findById_shouldUpdateEmptyCredentials() {
        String paymentProvider = "test provider";
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway(paymentProvider)
                .withServiceName("a cool service")
                .build());

        final Optional<GatewayAccountEntity> maybeGatewayAccount = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(maybeGatewayAccount.isPresent(), is(true));
        GatewayAccountEntity gatewayAccount = maybeGatewayAccount.get();

        assertThat(gatewayAccount.getCredentials(), is(emptyMap()));

        gatewayAccount.setCredentials(new HashMap<String, String>() {{
            put("username", "Username");
            put("password", "Password");
        }});

        gatewayAccountDao.merge(gatewayAccount);

        Optional<GatewayAccountEntity> serviceAccountMaybe = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(serviceAccountMaybe.isPresent(), is(true));
        Map<String, String> credentialsMap = serviceAccountMaybe.get().getCredentials();
        assertThat(credentialsMap, hasEntry("username", "Username"));
        assertThat(credentialsMap, hasEntry("password", "Password"));
    }

    @Test
    public void findById_shouldUpdateAndRetrieveCredentialsWithSpecialCharacters() {
        String paymentProvider = "test provider";
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway(paymentProvider)
                .withServiceName("a cool service")
                .build());

        String aUserNameWithSpecialChars = "someone@some{[]where&^%>?\\/";
        String aPasswordWithSpecialChars = "56g%%Bqv\\>/<wdUpi@#bh{[}]6JV+8w";
        var credMap = Map.of("username", aUserNameWithSpecialChars, "password", aPasswordWithSpecialChars);

        final Optional<GatewayAccountEntity> maybeGatewayAccount = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(maybeGatewayAccount.isPresent(), is(true));
        GatewayAccountEntity gatewayAccount = maybeGatewayAccount.get();
        gatewayAccount.setCredentials(credMap);

        gatewayAccountDao.merge(gatewayAccount);

        Optional<GatewayAccountEntity> serviceAccountMaybe = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(serviceAccountMaybe.isPresent(), is(true));
        Map<String, String> credentialsMap = serviceAccountMaybe.get().getCredentials();
        assertThat(credentialsMap, hasEntry("username", aUserNameWithSpecialChars));
        assertThat(credentialsMap, hasEntry("password", aPasswordWithSpecialChars));
    }

    @Test
    public void findById_shouldFindAccountInfoByIdWhenFindingByIdReturningGatewayAccount() {
        String paymentProvider = "test provider";
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway(paymentProvider)
                .withServiceName("a cool service")
                .build());

        Optional<GatewayAccountEntity> gatewayAccountOpt = gatewayAccountDao.findById(gatewayAccountId);

        assertTrue(gatewayAccountOpt.isPresent());
        GatewayAccountEntity gatewayAccount = gatewayAccountOpt.get();
        assertThat(gatewayAccount.getGatewayName(), is(paymentProvider));
        assertThat(gatewayAccount.getCredentials().size(), is(0));
    }

    @Test
    public void findById_shouldGetCredentialsWhenFindingGatewayAccountById() {
        String paymentProvider = "test provider";
        HashMap<String, String> credentials = new HashMap<>();
        credentials.put("username", "Username");
        credentials.put("password", "Password");

        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway(paymentProvider)
                .withCredentials(credentials)
                .build());

        Optional<GatewayAccountEntity> gatewayAccount = gatewayAccountDao.findById(gatewayAccountId);

        assertThat(gatewayAccount.isPresent(), is(true));
        Map<String, String> accountCredentials = gatewayAccount.get().getCredentials();
        assertThat(accountCredentials, hasEntry("username", "Username"));
        assertThat(accountCredentials, hasEntry("password", "Password"));
    }

    @Test
    public void shouldSaveNotificationCredentials() {
        String paymentProvider = "test provider";
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway(paymentProvider)
                .withServiceName("a cool service")
                .build());

        final Optional<GatewayAccountEntity> maybeGatewayAccount = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(maybeGatewayAccount.isPresent(), is(true));
        GatewayAccountEntity gatewayAccount = maybeGatewayAccount.get();

        NotificationCredentials notificationCredentials = new NotificationCredentials(gatewayAccount);
        notificationCredentials.setPassword("password");
        notificationCredentials.setUserName("username");
        gatewayAccount.setNotificationCredentials(notificationCredentials);

        gatewayAccountDao.merge(gatewayAccount);

        final Optional<GatewayAccountEntity> maybeGatewayAccount_2 = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(maybeGatewayAccount_2.isPresent(), is(true));
        GatewayAccountEntity retrievedGatewayAccount = maybeGatewayAccount.get();

        assertNotNull(retrievedGatewayAccount.getNotificationCredentials());
        assertThat(retrievedGatewayAccount.getNotificationCredentials().getUserName(), is("username"));
        assertThat(retrievedGatewayAccount.getNotificationCredentials().getPassword(), is("password"));
    }

    @Test
    public void shouldListAllAccounts() {
        final long gatewayAccountId_1 = nextLong();
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_1))
                .withPaymentGateway("provider-1")
                .withCredentials(Map.of("user", "fuser", "password", "word"))
                .withServiceName("service-name-1")
                .withDescription("description-1")
                .withAnalyticsId("analytics-id-1")
                .withCorporateCreditCardSurchargeAmount(100)
                .withCorporateDebitCardSurchargeAmount(200)
                .withCorporatePrepaidCreditCardSurchargeAmount(300)
                .withCorporatePrepaidDebitCardSurchargeAmount(400)
                .build());
        final long gatewayAccountId_2 = nextLong();
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_2))
                .withPaymentGateway("provider-2")
                .withServiceName("service-name-2")
                .withDescription("description-2")
                .withAnalyticsId("analytics-id-2")
                .withCorporateCreditCardSurchargeAmount(250)
                .withCorporateDebitCardSurchargeAmount(50)
                .withCorporatePrepaidCreditCardSurchargeAmount(250)
                .withCorporatePrepaidDebitCardSurchargeAmount(50)
                .build());
        final long gatewayAccountId_3 = nextLong();
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_3))
                .withPaymentGateway("provider-3")
                .withServiceName("service-name-3")
                .withDescription("description-3")
                .withAnalyticsId("analytics-id-3")
                .withProviderUrlType(GatewayAccountEntity.Type.LIVE)
                .build());

        List<GatewayAccountEntity> gatewayAccounts = gatewayAccountDao.listAll();

        assertEquals(3, gatewayAccounts.size());

        List<GatewayAccountEntity> gatewayAccountWithId_1 = gatewayAccounts.stream().filter(ga -> ga.getId() == gatewayAccountId_1).collect(Collectors.toList());
        assertEquals(gatewayAccountWithId_1.size(), 1);
        GatewayAccountEntity gatewayAccountEntity = gatewayAccountWithId_1.get(0);
        assertThat(gatewayAccountEntity.getId(), is(gatewayAccountId_1));
        assertEquals("provider-1", gatewayAccountEntity.getGatewayName());
        assertEquals("description-1", gatewayAccountEntity.getDescription());
        assertEquals("service-name-1", gatewayAccountEntity.getServiceName());
        assertEquals(TEST.toString(), gatewayAccountEntity.getType());
        assertEquals("analytics-id-1", gatewayAccountEntity.getAnalyticsId());
        assertEquals(100L, gatewayAccountEntity.getCorporateNonPrepaidCreditCardSurchargeAmount());
        assertEquals(200L, gatewayAccountEntity.getCorporateNonPrepaidDebitCardSurchargeAmount());
        assertEquals(300L, gatewayAccountEntity.getCorporatePrepaidCreditCardSurchargeAmount());
        assertEquals(400L, gatewayAccountEntity.getCorporatePrepaidDebitCardSurchargeAmount());

        List<GatewayAccountEntity> gatewayAccountWithId_2 = gatewayAccounts.stream().filter(ga -> ga.getId() == gatewayAccountId_2).collect(Collectors.toList());
        assertEquals(gatewayAccountWithId_2.size(), 1);
        gatewayAccountEntity = gatewayAccountWithId_2.get(0);
        assertEquals("provider-2", gatewayAccountEntity.getGatewayName());
        assertEquals(250L, gatewayAccountEntity.getCorporateNonPrepaidCreditCardSurchargeAmount());
        assertEquals(50L, gatewayAccountEntity.getCorporateNonPrepaidDebitCardSurchargeAmount());
        assertThat(gatewayAccountEntity.getId(), is(gatewayAccountId_2));

        List<GatewayAccountEntity> gatewayAccountWithId_3 = gatewayAccounts.stream().filter(ga -> ga.getId() == gatewayAccountId_3).collect(Collectors.toList());
        assertEquals(gatewayAccountWithId_3.size(), 1);
        gatewayAccountEntity = gatewayAccountWithId_3.get(0);
        assertEquals("provider-3", gatewayAccountEntity.getGatewayName());
        assertEquals(0L, gatewayAccountEntity.getCorporatePrepaidCreditCardSurchargeAmount());
        assertEquals(0L, gatewayAccountEntity.getCorporatePrepaidDebitCardSurchargeAmount());
        assertThat(gatewayAccountEntity.getId(), is(gatewayAccountId_3));
    }
    
    
    @Test
    public void shouldListASubsetOfAccountsSingle() {
        long gatewayAccountId_1 = nextLong();
        databaseTestHelper.addGatewayAccount(
                anAddGatewayAccountParams()
                        .withAccountId(String.valueOf(gatewayAccountId_1))
                        .withPaymentGateway("provider-1")
                        .withCredentials(Map.of("user", "fuser","password", "word"))
                        .withServiceName("service-name-1")
                        .withDescription("description-1")
                        .withAnalyticsId("analytics-id-1")
                        .build());
        long gatewayAccountId_2 = nextLong();
        databaseTestHelper.addGatewayAccount(
                anAddGatewayAccountParams()
                        .withAccountId(String.valueOf(gatewayAccountId_2))
                        .withPaymentGateway("provider-2")
                        .withServiceName("service-name-2")
                        .withDescription("description-2")
                        .withAnalyticsId("analytics-id-2")
                        .build());

        List<Long> accountIds = Collections.singletonList(gatewayAccountId_2);
        List<GatewayAccountEntity> gatewayAccounts = gatewayAccountDao.list(accountIds);

        assertEquals(1, gatewayAccounts.size());
        assertThat(gatewayAccounts.get(0).getId(), is(gatewayAccountId_2));
        assertEquals("provider-2", gatewayAccounts.get(0).getGatewayName());
        assertEquals("description-2", gatewayAccounts.get(0).getDescription());
        assertEquals("service-name-2", gatewayAccounts.get(0).getServiceName());
        assertEquals(TEST.toString(), gatewayAccounts.get(0).getType());
        assertEquals("analytics-id-2", gatewayAccounts.get(0).getAnalyticsId());
    }

    @Test
    public void shouldListASubsetOfAccountsMultiple() {
        final long gatewayAccountId_1 = nextLong();
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_1))
                .withPaymentGateway("provider-1")
                .withCredentials(Map.of("user", "fuser","password", "word"))
                .withServiceName("service-name-1")
                .withDescription("description-1")
                .withAnalyticsId("analytics-id-1")
                .build());
        final long gatewayAccountId_2 = gatewayAccountId_1 + 1;
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_2))
                .withPaymentGateway("provider-2")
                .withServiceName("service-name-2")
                .withDescription("description-2")
                .withAnalyticsId("analytics-id-2")
                .withCorporateCreditCardSurchargeAmount(100)
                .withCorporateDebitCardSurchargeAmount(200)
                .withCorporatePrepaidCreditCardSurchargeAmount(300)
                .withCorporatePrepaidDebitCardSurchargeAmount(400)
                .build());
        final long gatewayAccountId_3 = gatewayAccountId_2 + 1;
        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId_3))
                .withPaymentGateway("provider-3")
                .withServiceName("service-name-3")
                .withDescription("description-3")
                .withAnalyticsId("analytics-id-3")
                .build());

        List<Long> accountIds = Arrays.asList(gatewayAccountId_2, gatewayAccountId_3);
        List<GatewayAccountEntity> gatewayAccounts = gatewayAccountDao.list(accountIds);

        assertEquals(2, gatewayAccounts.size());

        assertThat(gatewayAccounts.get(0).getId(), is(gatewayAccountId_2));
        assertEquals("provider-2", gatewayAccounts.get(0).getGatewayName());
        assertEquals("description-2", gatewayAccounts.get(0).getDescription());
        assertEquals("service-name-2", gatewayAccounts.get(0).getServiceName());
        assertEquals(TEST.toString(), gatewayAccounts.get(0).getType());
        assertEquals("analytics-id-2", gatewayAccounts.get(0).getAnalyticsId());
        assertEquals(100, gatewayAccounts.get(0).getCorporateNonPrepaidCreditCardSurchargeAmount());
        assertEquals(200L, gatewayAccounts.get(0).getCorporateNonPrepaidDebitCardSurchargeAmount());
        assertEquals(300L, gatewayAccounts.get(0).getCorporatePrepaidCreditCardSurchargeAmount());
        assertEquals(400L, gatewayAccounts.get(0).getCorporatePrepaidDebitCardSurchargeAmount());

        assertThat(gatewayAccounts.get(1).getId(), is(gatewayAccountId_3));
        assertEquals("provider-3", gatewayAccounts.get(1).getGatewayName());
        assertEquals("description-3", gatewayAccounts.get(1).getDescription());
        assertEquals("service-name-3", gatewayAccounts.get(1).getServiceName());
        assertEquals(TEST.toString(), gatewayAccounts.get(1).getType());
        assertEquals("analytics-id-3", gatewayAccounts.get(1).getAnalyticsId());
    }

    @Test
    public void shouldSaveNotifySettings() {
        String fuser = "fuser";
        String notifyAPIToken = "a_token";
        String notifyTemplateId = "a_template_id";

        databaseTestHelper.addGatewayAccount(anAddGatewayAccountParams()
                .withAccountId(String.valueOf(gatewayAccountId))
                .withPaymentGateway("provider-1")
                .withCredentials(Map.of("user", fuser, "password", "word"))
                .withServiceName("service-name-1")
                .withDescription("description-1")
                .withAnalyticsId("analytics-id-1")
                .build());
        Optional<GatewayAccountEntity> gatewayAccountOptional = gatewayAccountDao.findById(gatewayAccountId);
        assertThat(gatewayAccountOptional.isPresent(), is(true));
        GatewayAccountEntity gatewayAccountEntity = gatewayAccountOptional.get();
        assertThat(gatewayAccountEntity.getNotifySettings(), is(nullValue()));
        var notifySettings = Map.of("notify_api_token", notifyAPIToken, "notify_template_id", notifyTemplateId);
        gatewayAccountEntity.setNotifySettings(notifySettings);
        gatewayAccountDao.merge(gatewayAccountEntity);

        Map<String, String> storedNotifySettings = databaseTestHelper.getNotifySettings(gatewayAccountId);

        assertThat(storedNotifySettings.size(), is(2));
        assertThat(storedNotifySettings.get("notify_api_token"), is(notifyAPIToken));
        assertThat(storedNotifySettings.get("notify_template_id"), is(notifyTemplateId));
    }

    private DatabaseFixtures.TestAccount createAccountRecordWithCards(CardTypeEntity... cardTypes) {
        return databaseFixtures
                .aTestAccount()
                .withCardTypeEntities(Arrays.asList(cardTypes))
                .insert();
    }

    private DatabaseFixtures.TestAccount createAccountRecordWithCorporateSurcharges() {
        return databaseFixtures
                .aTestAccount()
                .withCorporateCreditCardSurchargeAmount(250L)
                .withCorporateDebitCardSurchargeAmount(50L)
                .withCorporatePrepaidCreditCardSurchargeAmount(250L)
                .withCorporatePrepaidDebitCardSurchargeAmount(50L)
                .insert();
    }
}
