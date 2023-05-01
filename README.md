# search-engine

Приложение для быстрого поиска страниц.
Поиск проводиться по индексированным сайтам, список которых устанавлевает пользователь.
Запуск индексации осуществляется пользователем.Запрос состоит из текста, который есть на странице.
В меню доступны следующие действия:
  - "DASHBOARD" просмотр статистики. Результаты статистики можно увидеть после обновления страницы.  
  - "MANAGEMENT" запуск и остановка индексации. После остановки индексации появляется поле "add/update" где можно добавить или обновить страницу.
  Доменное имя страницы должно соответствовать одному из имен сайтов установленного списка. Так-же можно запустить индексацию заново. Если сайт был
  проиндексирован и в момент переиндексации недоступен, старые данные сохраняються.
   
  - "SEARCH" позволяет сделать запрос по всем либо по одному сайту из списка. Результаты поиска пявляються после отправки запроса.


## В приложении использовались следующие технологии:
  
- Framework Spring Boot
  - JPA
  - Apache Tom Cat
  - HikkariPool
  - JDBC
- ForkJoinPool
- лемматизация apache lucene
- Jsoup
 

## Установка и запуск приложения.
- Клонируем ссылку на репозиторий.
- Открываем проект в среде разработки, например IntelliJIdea.
- В конфигурационном файле "application.yaml" устанавливаем
  - данные порта для этого приложения. 
  - данные подключения к базе данных  datasource: "user", "password" и "url" путь к MySQL серверу.
  - Так же указываем данные сайтов sites: "url" и "name".


Скриншот файла application.yaml с настройками
![Снимок экрана (97)](https://user-images.githubusercontent.com/95944672/235534425-49381621-2942-49ba-9c78-f64aa2639ce6.png) 

- в правом верхнем углу, кликаем по "Maven", выбираем "LifeCicle" 
    и кликаем
 "package". В папке "target" появиться файл.jar, как в скриншоте.

- Открываем командную строку и запускаем файл.jar
- Меню управления откроется в браузере устройства по http://localhost/
  
  ## Возможные проблемы с запуском
- Для запуска используйте ту же версию Java которая компилировала архив. Это может быть Java используемая вашей средой разработки.
  
  
