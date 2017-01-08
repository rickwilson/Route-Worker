package core.services;

import core.Application;
import org.javamoney.moneta.Money;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.MonetaryRounding;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRateProvider;
import javax.money.convert.MonetaryConversions;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;

@Service
public class CurrencyService {

    private final HashMap<String, String> countryCodeCurrencyCodeMap;

//    @Autowired
    public CurrencyService() {
        countryCodeCurrencyCodeMap = new HashMap<>();
        for (Locale locale : Locale.getAvailableLocales()) {
            try {
                Currency currency = Currency.getInstance(locale);
                countryCodeCurrencyCodeMap.put(locale.getISO3Country(), currency.getCurrencyCode());
            } catch (Exception e) {
            }
        }
    }

    @Cacheable("exchangeRate1")
    public String exchangeToUSDWithCountryCode(String termCountryCode, BigDecimal amount) {
        return exchangeToUSDWithCurrencyCode(countryCodeCurrencyCodeMap.get(termCountryCode),amount);
    }

    @Cacheable("exchangeRate2")
    public String exchangeToUSDWithCurrencyCode(String termCurrencyCode, BigDecimal amount) {
        CurrencyUnit currencyUSD = Monetary.getCurrency("USD");
        CurrencyUnit termCurrency = Monetary.getCurrency(termCurrencyCode);

        CurrencyConversion usdConversion = MonetaryConversions.getConversion(currencyUSD);

        MonetaryAmount termDollars = Money.of(amount, termCurrency);
        MonetaryAmount usDollar = termDollars.with(usdConversion);

        MonetaryRounding rounding = Monetary.getRounding(currencyUSD);
        MonetaryAmount roundedUSD = usDollar.with(rounding);
        return roundedUSD.getNumber().toString();
    }

    @Cacheable("exchangeRate3")
    public String getExchangeRate(String termCurrencyCode) {
        ExchangeRateProvider ecbExchangeRateProvider = MonetaryConversions.getExchangeRateProvider();
        return ecbExchangeRateProvider.getExchangeRate("USD",termCurrencyCode).getFactor().toString();
    }

    @CacheEvict(value = { "exchangeRate1","exchangeRate2","exchangeRate3" }, allEntries = true)
    public void evictExchangeRateCaches() {
    }

    @Scheduled(fixedRate = 21600000) // 23 hours
    public void runEvict() {
        evictExchangeRateCaches();
    }
}
